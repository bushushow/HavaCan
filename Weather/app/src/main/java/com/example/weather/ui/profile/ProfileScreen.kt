package com.example.weather.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weather.BuildConfig
import com.example.weather.R
import com.example.weather.ui.home.components.SoftCard
import com.example.weather.ui.theme.HavaBlue
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.NavySoft
import com.example.weather.ui.theme.RiskRed
import com.example.weather.ui.voice.TtsStatus
import com.example.weather.ui.voice.VoiceGuideState

/** Profil: settings (voice use first) and where the data comes from. */
@Composable
fun ProfileScreen(
    state: VoiceGuideState,
    onGuideChange: (Boolean) -> Unit,
    onSpeakTypedChange: (Boolean) -> Unit,
    onTryGuide: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.nav_profile), style = MaterialTheme.typography.headlineMedium, color = Navy, modifier = Modifier.semantics { heading() })

        SectionTitle(stringResource(R.string.settings_title))
        SoftCard {
            Column(Modifier.padding(vertical = 8.dp)) {
                SwitchRow(
                    title = stringResource(R.string.voice_setting_title),
                    body = stringResource(R.string.voice_setting_body),
                    checked = state.settings.guideEnabled,
                    enabled = state.recognitionAvailable || state.settings.guideEnabled,
                    onChange = onGuideChange,
                )
                val problems = listOfNotNull(
                    stringResource(R.string.voice_no_recognizer).takeIf { !state.recognitionAvailable },
                    stringResource(R.string.voice_no_turkish_tts).takeIf { state.tts == TtsStatus.NoTurkish },
                    stringResource(R.string.voice_no_tts).takeIf { state.tts == TtsStatus.Unavailable },
                )
                problems.forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = RiskRed, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
                if (state.settings.guideEnabled) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.voice_howto_title), style = MaterialTheme.typography.titleSmall, color = Navy, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.voice_howto_body), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                        if (state.screenReader) {
                            Text(stringResource(R.string.voice_talkback_note), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                        }
                        Button(
                            onClick = onTryGuide,
                            colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) { Text(stringResource(R.string.voice_try)) }
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = Navy.copy(alpha = 0.08f))
                SwitchRow(
                    title = stringResource(R.string.voice_speak_typed_title),
                    body = stringResource(R.string.voice_speak_typed_body),
                    checked = state.settings.speakTypedReplies,
                    enabled = true,
                    onChange = onSpeakTypedChange,
                )
            }
        }

        SectionTitle(stringResource(R.string.about_title))
        SoftCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleSmall, color = Navy, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.about_weather_data), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                Text(stringResource(R.string.about_place_data), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                Text(stringResource(R.string.about_font), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = NavySoft, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
}

/** The whole row is the switch, so TalkBack reads title, explanation and on/off as one control. */
@Composable
private fun SwitchRow(title: String, body: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Navy, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = NavySoft)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = HavaBlue),
        )
    }
}
