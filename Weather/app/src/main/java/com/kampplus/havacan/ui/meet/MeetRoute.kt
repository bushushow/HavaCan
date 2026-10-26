package com.kampplus.havacan.ui.meet

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.havacan.model.City
import com.kampplus.havacan.ui.theme.LocalDisplaySettings

/**
 * Weather Meet inside the Planlar tab. Each opening ([token]) gets a fresh ViewModel; sharing
 * hands the invitation to Android's share sheet (WhatsApp, SMS…), so nothing is sent by itself.
 */
@Composable
fun MeetRoute(token: Long, clock: () -> Float, onPickCity: (onPicked: (City) -> Unit) -> Unit, onSaved: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val unit = LocalDisplaySettings.current.unit
    val viewModel: MeetViewModel = viewModel(key = "meet-$token", factory = MeetViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Ask for the notification permission once the meet-up is saved, like a new plan does
    LaunchedEffect(state.saved) { if (state.saved != null) onSaved() }

    MeetScreen(
        state = state,
        clock = clock,
        actions = MeetActions(
            onBack = onClose,
            onTitle = viewModel::setTitle,
            onAddPerson = viewModel::addPerson,
            onRemovePerson = viewModel::removePerson,
            onPickCity = onPickCity,
            onDate = viewModel::selectDate,
            onToggleWindow = viewModel::toggleWindow,
            onPlace = viewModel::setPlace,
            onOption = viewModel::selectOption,
            onRetry = viewModel::retry,
            onSave = viewModel::save,
            onShare = {
                val option = state.chosen ?: return@MeetActions
                val title = state.saved?.title ?: state.title.trim().ifBlank { MeetViewModel.defaultTitle(state.people) }
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, MeetViewModel.inviteText(title, option, unit))
                context.startActivity(Intent.createChooser(send, null))
            },
            onDone = onClose
        )
    )
}
