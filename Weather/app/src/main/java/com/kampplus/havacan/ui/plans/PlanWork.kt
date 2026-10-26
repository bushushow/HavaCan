package com.kampplus.havacan.ui.plans

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kampplus.havacan.MainActivity
import com.kampplus.havacan.R
import com.kampplus.havacan.data.PlanScheduler
import com.kampplus.havacan.di.AppContainer
import com.kampplus.havacan.domain.plans.PlanAdvisor
import com.kampplus.havacan.domain.plans.PlanOutlook
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.TemperatureUnit
import com.kampplus.havacan.model.deg
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val KEY_PLAN_ID = "planId"
private const val WATCH_WORK = "plan-watch"
const val EXTRA_PLAN_ID = "com.kampplus.havacan.PLAN_ID"

/** Reminders as one-off jobs at "plan time minus reminder"; followed plans checked every 3 hours. */
class WorkPlanScheduler(private val context: Context) : PlanScheduler {
    private val work get() = WorkManager.getInstance(context)

    override fun schedule(plan: Plan) {
        val minutes = plan.reminderMinutesBefore
        val fireAt = minutes?.let { plan.dateTime.minusMinutes(it) }
        val delay = fireAt?.let { Duration.between(LocalDateTime.now(), it) }
        if (delay != null && !delay.isNegative) {
            work.enqueueUniqueWork(
                reminderName(plan.id),
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PlanReminderWorker>()
                    .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(KEY_PLAN_ID to plan.id))
                    .build()
            )
        } else {
            work.cancelUniqueWork(reminderName(plan.id))
        }
        if (plan.follow) {
            work.enqueueUniquePeriodicWork(
                WATCH_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PlanWatchWorker>(3, TimeUnit.HOURS)
                    .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                    .build()
            )
        }
    }

    override fun cancel(planId: String) {
        work.cancelUniqueWork(reminderName(planId))
    }

    /**
     * WorkManager stores jobs by worker class name; after the code package moved from
     * com.example.weather to com.kampplus.havacan, jobs saved under the old names can't run.
     * Re-enqueues every plan's jobs once under the current names.
     */
    suspend fun migrateWorkOnce() {
        val prefs = context.getSharedPreferences("havacan", Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_WORK_VERSION, 0) >= WORK_VERSION) return
        work.cancelUniqueWork(WATCH_WORK)
        val plans = AppContainer.get(context).plans
        plans.load()
        plans.plans.value.forEach(::schedule)
        prefs.edit().putInt(KEY_WORK_VERSION, WORK_VERSION).apply()
    }

    private fun reminderName(id: String) = "reminder-$id"

    private companion object {
        const val KEY_WORK_VERSION = "work_version"
        const val WORK_VERSION = 2
    }
}

class PlanReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = AppContainer.get(applicationContext)
        val unit = graph.profile.prefs.value.unit
        val plan = graph.plans.get(inputData.getString(KEY_PLAN_ID) ?: return Result.success()) ?: return Result.success()
        // Profil > Bildirimler: runs and other plans can be silenced separately
        val prefs = graph.profile.prefs.value
        if (if (plan.isRun) !prefs.notifyRuns else !prefs.notifyPlans) return Result.success()
        val forecast = runCatching { graph.weather.forecast(plan.city.latitude, plan.city.longitude) }.getOrNull()
        val outlook = PlanAdvisor.outlook(plan, forecast, LocalDateTime.now())
        PlanNotifications.show(
            applicationContext,
            plan,
            title = "${plan.title} · ${plan.timeLabel}",
            text = outlook.summary(unit),
            kind = PlanNotifications.Kind.Reminder
        )
        return Result.success()
    }
}

/** Re-checks followed plans and speaks up only when the risk state changes. */
class PlanWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = AppContainer.get(applicationContext)
        val unit = graph.profile.prefs.value.unit
        graph.plans.load()
        val now = LocalDateTime.now()
        val prefs = graph.profile.prefs.value
        for (plan in graph.plans.plans.value.filter { it.follow }) {
            val forecast = runCatching { graph.weather.forecast(plan.city.latitude, plan.city.longitude, forceRefresh = true) }
                .getOrNull() ?: continue
            val outlook = PlanAdvisor.outlook(plan, forecast, now)
            val state = when (outlook) {
                is PlanOutlook.Risk -> outlook.reason.name
                is PlanOutlook.Fine -> "Fine"
                else -> continue
            }
            if (state == plan.lastNotified) continue
            when {
                !prefs.notifyWeather -> Unit // Profil > Bildirimler: weather alerts off, state still tracked
                outlook is PlanOutlook.Risk -> {
                    // Plan B suggestion only when wanted; the alert itself still opens Plan B
                    val planB = if (prefs.notifyPlanB) {
                        PlanAdvisor.alternative(plan, forecast, now)?.let {
                            " · Plan B: ${it.hourLabel} daha uygun"
                        }
                    } else {
                        null
                    }
                    PlanNotifications.show(
                        applicationContext,
                        plan,
                        "Planın riskte: ${plan.title}",
                        outlook.summary(unit) + planB.orEmpty()
                    )
                }
                plan.lastNotified != null -> PlanNotifications.show(
                    applicationContext,
                    plan,
                    "Hava düzeldi: ${plan.title}",
                    outlook.summary(unit)
                )
            }
            graph.plans.upsert(plan.copy(lastNotified = state))
        }
        return Result.success()
    }
}

object PlanNotifications {
    private const val CHANNEL = "plans"

    fun canNotify(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Separate ids so a reminder and a risk alert for the same plan don't replace each other. */
    enum class Kind { Reminder, Watch }

    fun show(context: Context, plan: Plan, title: String, text: String, kind: Kind = Kind.Watch) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.plans_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context,
            plan.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_PLAN_ID, plan.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // checked by canNotify
        NotificationManagerCompat.from(context).notify(plan.id.hashCode() * 2 + kind.ordinal, notification)
    }
}

/** One line describing a plan's weather, shared by the screen and notifications. */
fun PlanOutlook.summary(unit: TemperatureUnit): String = when (this) {
    is PlanOutlook.Risk -> {
        val chance = hour.precipitationProbability?.let { " · yağış ihtimali %$it" }.orEmpty()
        "${reason.label}$chance"
    }
    is PlanOutlook.Fine -> "Hava uygun · ${hour.temperature.deg(unit)}°, ${hour.condition.label}"
    is PlanOutlook.NoForecastYet -> "Tahmin ${daysUntil - 15} gün sonra gelecek"
    PlanOutlook.Unknown -> "Hava bilgisi şu an alınamadı"
    PlanOutlook.Past -> "Geçti"
}
