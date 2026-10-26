package com.kampplus.havacan.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.PlanSource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalendarEvent(val id: Long, val title: String, val start: LocalDateTime, val location: String?)

/**
 * The phone's calendar provider, which includes Google Calendar once the account syncs to the
 * device. Reading needs READ_CALENDAR; adding a plan opens the calendar app's own editor, so
 * nothing is written to the calendar without the user confirming it there.
 */
class CalendarGateway(private val context: Context) {

    fun hasReadPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Timed (not all-day) events starting within the next [days] days. */
    suspend fun upcomingEvents(days: Long = 14): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) return@withContext emptyList()
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, now + days * 24 * 60 * 60 * 1000)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY
        )
        val events = mutableListOf<CalendarEvent>()
        context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) {
                if (c.getInt(4) == 1) continue
                events += CalendarEvent(
                    id = c.getLong(0),
                    title = c.getString(1).orEmpty().ifBlank { "Etkinlik" },
                    start = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(2)), zone),
                    location = c.getString(3)?.takeIf { it.isNotBlank() }
                )
            }
        }
        events
    }

    /** Calendar app's "new event" screen, pre-filled with the plan. */
    fun insertIntent(plan: Plan): Intent {
        val start = plan.dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, plan.title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + (plan.run?.durationMinutes ?: 60) * 60 * 1000L)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, listOfNotNull(plan.placeName, plan.city.name).joinToString(", "))
    }

    /** Opens an imported event in the calendar app, where the user confirms any change. */
    fun editIntent(eventId: Long): Intent =
        Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId))

    companion object {
        /** Imported events become plans; their location defaults to [city] until the user edits it. */
        fun toPlan(event: CalendarEvent, city: City, existing: Plan?): Plan = Plan(
            id = "cal-${event.id}",
            title = event.title,
            date = event.start.toLocalDate().toString(),
            time = event.start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
            city = existing?.city ?: city,
            placeName = event.location,
            type = existing?.type ?: com.kampplus.havacan.model.PlanType.Outdoor,
            follow = existing?.follow ?: true,
            reminder = existing?.reminder ?: com.kampplus.havacan.model.Reminder.None,
            source = PlanSource.Calendar,
            calendarEventId = event.id,
            lastNotified = existing?.lastNotified
        )
    }
}
