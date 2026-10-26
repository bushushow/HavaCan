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

/**
 * Koşu Planla, a navigation destination. Its ViewModel belongs to the back stack entry, so each
 * opening starts fresh while rotation keeps the run being filled in; the route's planId and
 * start arguments reach the ViewModel through SavedStateHandle.
 */
@Composable
fun RunPlannerRoute(
    clock: () -> Float,
    reduceMotion: Boolean,
    onPickCity: (onPicked: (City) -> Unit) -> Unit,
    onClose: () -> Unit,
    onSaved: (RunSaved) -> Unit,
    onDeleted: () -> Unit,
    viewModel: RunPlannerViewModel = viewModel(factory = RunPlannerViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
