package com.kampplus.havacan.ui.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.kampplus.havacan.R

fun hasMicrophonePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/**
 * Returns an action that runs [onGranted] once the microphone may be used. The first time it
 * explains why (in plain words, as a dialog TalkBack reads out) before Android asks.
 */
@Composable
fun rememberMicrophoneAccess(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val granted by rememberUpdatedState(onGranted)
    var explain by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) granted() else Toast.makeText(context, R.string.mic_denied, Toast.LENGTH_LONG).show()
    }
    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text(stringResource(R.string.mic_permission_title)) },
            text = { Text(stringResource(R.string.mic_permission_body)) },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text(stringResource(R.string.mic_permission_allow)) }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text(stringResource(R.string.not_now)) } }
        )
    }
    return { if (hasMicrophonePermission(context)) granted() else explain = true }
}
