package com.kampplus.havacan.ui.plans

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.havacan.R
import com.kampplus.havacan.data.AppGraph
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.PlanSource
import com.kampplus.havacan.ui.run.RunSaved
import java.time.LocalDateTime

/** Which plans screen is showing; a plan id travels with the sub-screens. */
sealed interface PlansDestination {
    data object List : PlansDestination
    data class Editor(val planId: String?) : PlansDestination
    data class PlanB(val planId: String) : PlansDestination
}

/**
 * The Planlar tab: list, editor and Plan B, plus the system pieces they need (calendar and
 * notification permissions, the calendar app, the Kamp+ city picker for a plan's location).
 */
@Composable
fun PlansRoute(
    clock: () -> Float,
    openPlanId: String?,
    onOpenPlanHandled: () -> Unit,
    onCityClick: () -> Unit,
    onBellClick: () -> Unit,
    onPickPlanCity: (onPicked: (City) -> Unit) -> Unit,
    onShowBottomBar: (Boolean) -> Unit,
    onOpenRunPlan: (planId: String) -> Unit,
    runNotice: RunSaved?,
    onRunNoticeShown: () -> Unit,
    viewModel: PlansViewModel = viewModel(factory = PlansViewModel.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable(stateSaver = DestinationSaver) { mutableStateOf<PlansDestination>(PlansDestination.List) }
    // Location chosen in the editor; survives the trip through the city picker
    var editorCity by rememberSaveable(stateSaver = CitySaver) { mutableStateOf<City?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(openPlanId) {
        if (openPlanId != null) {
            destination = PlansDestination.PlanB(openPlanId)
            onOpenPlanHandled()
        }
    }
    LaunchedEffect(destination) { onShowBottomBar(destination == PlansDestination.List) }
    BackHandler(enabled = destination != PlansDestination.List) {
        destination = PlansDestination.List
        editorCity = null
    }

    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onCalendarPermissionResult(granted)
        if (!granted) Toast.makeText(context, R.string.calendar_denied, Toast.LENGTH_LONG).show()
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // A run saved in Koşu Planla: confirm it here and offer the phone calendar (the user confirms there)
    val snackbar = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.run_saved_snackbar)
    val addToCalendar = stringResource(R.string.add_to_calendar_short)
    var pendingNotice by remember { mutableStateOf<RunSaved?>(null) }
    LaunchedEffect(runNotice) {
        if (runNotice != null) {
            pendingNotice = runNotice
            onRunNoticeShown()
        }
    }
    LaunchedEffect(pendingNotice) {
        val notice = pendingNotice ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            savedMessage,
            actionLabel = addToCalendar,
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            runCatching { context.startActivity(AppGraph.get(context).calendar.insertIntent(notice.plan)) }
                .onFailure {
                    if (it is ActivityNotFoundException) Toast.makeText(context, "Takvim uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                val forward = targetState != PlansDestination.List
                (slideInHorizontally(tween(320)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(250))) togetherWith
                    (slideOutHorizontally(tween(320)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)))
            },
            label = "plans"
        ) { dest ->
            when (dest) {
                PlansDestination.List -> PlansScreen(
                    state = state,
                    clock = clock,
                    onCityClick = onCityClick,
                    onBellClick = onBellClick,
                    onConnectCalendar = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
                    onSelectDate = viewModel::selectDate,
                    onOpenPlan = { if (it.plan.isRun) onOpenRunPlan(it.plan.id) else destination = PlansDestination.Editor(it.plan.id) },
                    onPlanB = { destination = PlansDestination.PlanB(it.plan.id) },
                    onAddPlan = {
                        editorCity = null
                        destination = PlansDestination.Editor(null)
                    }
                )
                is PlansDestination.Editor -> {
                    val existing = dest.planId?.let { id -> state.items.firstOrNull { it.plan.id == id }?.plan }
                    val initial = existing ?: rememberSaveable(saver = PlanSaver) { blankPlan(viewModel.newPlanId(), state.homeCity) }
                    PlanEditorScreen(
                        initial = initial,
                        isNew = existing == null,
                        city = editorCity ?: initial.city,
                        clock = clock,
                        onPickCity = { onPickPlanCity { picked -> editorCity = picked } },
                        onSave = { plan, addToCalendar ->
                            viewModel.save(plan)
                            if ((plan.follow || plan.reminderMinutesBefore != null) && Build.VERSION.SDK_INT >= 33 &&
                                !PlanNotifications.canNotify(context)
                            ) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (addToCalendar) {
                                runCatching { context.startActivity(AppGraph.get(context).calendar.insertIntent(plan)) }
                                    .onFailure {
                                        if (it is ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                "Takvim uygulaması bulunamadı",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            }
                            editorCity = null
                            destination = PlansDestination.List
                        },
                        onDelete = {
                            dest.planId?.let(viewModel::delete)
                            destination = PlansDestination.List
                        },
                        onBack = {
                            editorCity = null
                            destination = PlansDestination.List
                        }
                    )
                }
                is PlansDestination.PlanB -> {
                    val item = state.items.firstOrNull { it.plan.id == dest.planId }
                    if (item == null) {
                        // The plan is gone or no longer at risk; nothing to decide
                        LaunchedEffect(Unit) { destination = PlansDestination.List }
                    } else {
                        PlanBScreen(
                            item = item,
                            clock = clock,
                            onApply = { choice ->
                                when (choice) {
                                    PlanBChoice.Move -> item.alternative?.let { hour ->
                                        viewModel.moveTo(item.plan, hour)
                                        val eventId = item.plan.calendarEventId
                                        if (item.plan.source == PlanSource.Calendar && eventId != null) {
                                            runCatching { context.startActivity(AppGraph.get(context).calendar.editIntent(eventId)) }
                                        }
                                    }
                                    PlanBChoice.Indoor -> viewModel.moveIndoors(item.plan)
                                    PlanBChoice.Keep -> viewModel.keepAndFollow(item.plan)
                                }
                                destination = PlansDestination.List
                            },
                            onBack = { destination = PlansDestination.List }
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp))
    }
}

private fun blankPlan(id: String, city: City): Plan {
    // Next full hour, at least an hour from now
    val start = LocalDateTime.now().plusHours(2).withMinute(0)
    return Plan(id = id, title = "", date = start.toLocalDate().toString(), time = "%02d:00".format(start.hour), city = city)
}

private val gson = com.google.gson.Gson()

private val PlanSaver = androidx.compose.runtime.saveable.Saver<Plan, String>(
    save = { gson.toJson(it) },
    restore = { gson.fromJson(it, Plan::class.java) }
)

private val CitySaver = androidx.compose.runtime.saveable.Saver<City?, String>(
    save = { it?.let(gson::toJson) ?: "" },
    restore = { if (it.isEmpty()) null else gson.fromJson(it, City::class.java) }
)

private val DestinationSaver = androidx.compose.runtime.saveable.Saver<PlansDestination, String>(
    save = {
        when (it) {
            PlansDestination.List -> "list"
            is PlansDestination.Editor -> "editor:${it.planId.orEmpty()}"
            is PlansDestination.PlanB -> "planb:${it.planId}"
        }
    },
    restore = {
        when {
            it.startsWith("editor:") -> PlansDestination.Editor(it.removePrefix("editor:").ifEmpty { null })
            it.startsWith("planb:") -> PlansDestination.PlanB(it.removePrefix("planb:"))
            else -> PlansDestination.List
        }
    }
)
