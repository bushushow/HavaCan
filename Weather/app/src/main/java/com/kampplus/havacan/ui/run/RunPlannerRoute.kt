package com.kampplus.havacan.ui.run

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.havacan.model.City

/** Who opened Koşu Planla: a new run (maybe with the home screen's suggested start) or an existing one. */
data class RunPlannerRequest(val token: Long, val planId: String? = null, val start: String? = null)

/**
 * Koşu Planla as a full-screen layer over the tabs. Each [request] gets its own ViewModel, so
 * opening the planner again starts fresh while rotation keeps the one being filled in.
 */
@Composable
fun RunPlannerRoute(
    request: RunPlannerRequest,
    clock: () -> Float,
    reduceMotion: Boolean,
    onPickCity: (onPicked: (City) -> Unit) -> Unit,
    onClose: () -> Unit,
    onSaved: (RunSaved) -> Unit,
    onDeleted: () -> Unit
) {
    val owner = LocalViewModelStoreOwner.current
    val extras = remember(request) {
        MutableCreationExtras(
            (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: MutableCreationExtras()
        ).apply {
            set(
                DEFAULT_ARGS_KEY,
                bundleOf(RunPlannerViewModel.KEY_PLAN_ID to request.planId, RunPlannerViewModel.KEY_START to request.start)
            )
        }
    }
    val viewModel: RunPlannerViewModel = viewModel(key = "run-${request.token}", factory = RunPlannerViewModel.Factory, extras = extras)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onClose)

    // Reminders need the notification permission; ask first, save either way and say what happened
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.save() }
    val save = {
        if (Build.VERSION.SDK_INT >= 33 && viewModel.needsNotificationPermission()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.save()
        }
    }

    RunPlannerScreen(
        state = state,
        clock = clock,
        reduceMotion = reduceMotion,
        actions = RunPlannerActions(
            onBack = onClose,
            onDay = viewModel::selectDay,
            onDate = viewModel::selectDate,
            onSlot = viewModel::selectSlot,
            onStart = viewModel::selectStart,
            onRetry = viewModel::retry,
            onGoal = viewModel::selectGoal,
            onDuration = viewModel::setDuration,
            onPickCity = { onPickCity(viewModel::setCity) },
            onPlaceName = viewModel::setPlaceName,
            onReminderOn = viewModel::setReminderOn,
            onReminderMinutes = viewModel::setReminderMinutes,
            onFollow = viewModel::setFollow,
            onSave = save,
            onDelete = { viewModel.delete(onDeleted) },
            onSavedShown = onSaved
        )
    )
}
