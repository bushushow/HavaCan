package com.kampplus.havacan.ui.profile

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kampplus.havacan.BuildConfig
import com.kampplus.havacan.R
import com.kampplus.havacan.data.ActivityInterest
import com.kampplus.havacan.data.OutfitStyle
import com.kampplus.havacan.data.ProfilePrefs
import com.kampplus.havacan.model.TemperatureUnit
import com.kampplus.havacan.ui.common.OnSky
import com.kampplus.havacan.ui.common.OnSkySoft
import com.kampplus.havacan.ui.common.SkyHeader
import com.kampplus.havacan.ui.home.components.SoftCard
import com.kampplus.havacan.ui.plans.PlanNotifications
import com.kampplus.havacan.ui.theme.Hava
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueLight
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.RiskRed
import com.kampplus.havacan.ui.voice.TtsStatus
import com.kampplus.havacan.ui.voice.hasMicrophonePermission

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkyHeader(bleed = 20.dp) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Profile dön", tint = OnSky) }
                Text(title, style = MaterialTheme.typography.headlineSmall, color = OnSky, modifier = Modifier.semantics { heading() })
            }
        }
        content()
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun Note(text: String, color: androidx.compose.ui.graphics.Color = Hava.inkSoft) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
}

/** The whole row is the switch, so TalkBack reads title, explanation and state as one control. */
@Composable
private fun SwitchRow(title: String, body: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Hava.ink else Hava.inkSoft,
                fontWeight = FontWeight.Bold
            )
            body?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Hava.inkSoft) }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = HavaBlue))
    }
}

private fun Context.openSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun Context.appSettings() =
    openSafely(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

// region Şehir ve birimler

@Composable
internal fun CityUnitsPage(prefs: ProfilePrefs, hooks: ProfileHooks, onBack: () -> Unit, onUnit: (TemperatureUnit) -> Unit) {
    SubPage("Şehir ve birimler", onBack) {
        SoftCard { SettingsRow(R.drawable.ic_place, "Şehir", hooks.cityName, hooks.onPickCity) }
        SectionTitle("Sıcaklık birimi")
        SoftCard {
            Column(Modifier.selectableGroup()) {
                TemperatureUnit.entries.forEach { unit ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = prefs.unit == unit, role = Role.RadioButton) { onUnit(unit) }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = prefs.unit == unit, onClick = null)
                        Text(
                            "${unit.label} (${unit.symbol})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Hava.ink,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
        Note("Seçtiğin birim ana sayfada, planlarda, Koşu Planla'da, bildirimlerde ve HavaCan'ın yanıtlarında kullanılır.")
    }
}

// endregion

// region Aktivite ve kombin tercihleri

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipPicker(options: List<T>, selected: Set<T>, label: (T) -> String, onToggle: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            val on = option in selected
            FilterChip(
                selected = on,
                onClick = { onToggle(option) },
                label = { Text(label(option), style = MaterialTheme.typography.titleSmall) },
                leadingIcon = if (on) ({ Icon(painterResource(R.drawable.ic_check), null) }) else null,
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HavaBlueLight, selectedLabelColor = Hava.ink),
                modifier = Modifier.heightIn(min = 48.dp)
            )
        }
    }
}

@Composable
internal fun ActivitiesPage(prefs: ProfilePrefs, onBack: () -> Unit, update: ((ProfilePrefs) -> ProfilePrefs) -> Unit) {
    SubPage("Aktivite tercihlerim", onBack) {
        Note("Sevdiğin açık hava aktivitelerini seç; birden fazla seçebilirsin.")
        ChipPicker(ActivityInterest.entries, prefs.activities, { it.label }) { a ->
            update { p -> p.copy(activities = if (a in p.activities) p.activities - a else p.activities + a) }
        }
        if (prefs.activities.isNotEmpty()) {
            TextButton(onClick = {
                update { it.copy(activities = emptySet()) }
            }) { Text("Seçimleri temizle") }
        }
        Note("Bu telefonda saklanır. HavaCan'ın kişisel önerileri için hazırlandı; şu an önerileri henüz değiştirmiyor.")
    }
}

@Composable
internal fun StylesPage(prefs: ProfilePrefs, onBack: () -> Unit, update: ((ProfilePrefs) -> ProfilePrefs) -> Unit) {
    SubPage("Kombin tercihlerim", onBack) {
        Note("İsteğe bağlı. Tarzını istediğin gibi birleştir; seçmezsen HavaCan bir şey varsaymaz.")
        ChipPicker(OutfitStyle.entries, prefs.styles, { it.label }) { s ->
            update { p -> p.copy(styles = if (s in p.styles) p.styles - s else p.styles + s) }
        }
        if (prefs.styles.isNotEmpty()) TextButton(onClick = { update { it.copy(styles = emptySet()) } }) { Text("Tercihlerimi sil") }
        Note(
            "Dolabım'daki kıyafetlerinden ayrı, bu telefonda saklanır. Salla Kombin şu an yalnızca hava ve renk uyumuna bakıyor; tarz tercihleri henüz kombin seçimini değiştirmiyor."
        )
    }
}

// endregion

// region Bildirimler

@Composable
internal fun NotificationsPage(prefs: ProfilePrefs, onBack: () -> Unit, update: ((ProfilePrefs) -> ProfilePrefs) -> Unit) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(PlanNotifications.canNotify(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        allowed = ok
        if (!ok) {
            context.openSafely(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }
    SubPage("Bildirimler", onBack) {
        if (!allowed) {
            SoftCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Note("Android bildirim izni kapalı: aşağıdaki seçenekler açık olsa bile bildirim gelmez.", RiskRed)
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.openSafely(
                                    Intent(
                                        Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                    ).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) { Text("Bildirim iznini ver") }
                }
            }
        }
        SoftCard {
            Column {
                SwitchRow("Hava durumu uyarıları", "Takip ettiğin planın havası bozulunca ya da düzelince", prefs.notifyWeather) { v ->
                    update { it.copy(notifyWeather = v) }
                }
                RowDivider()
                SwitchRow("Plan hatırlatmaları", "Planından önce, seçtiğin sürede", prefs.notifyPlans) { v ->
                    update { it.copy(notifyPlans = v) }
                }
                RowDivider()
                SwitchRow("Koşu hatırlatmaları", "Koşu Planla'da kurduğun koşulardan önce", prefs.notifyRuns) { v ->
                    update { it.copy(notifyRuns = v) }
                }
                RowDivider()
                SwitchRow(
                    "Plan B önerileri",
                    if (prefs.notifyWeather) "Hava uyarısında daha uygun saati de söyle" else "Hava durumu uyarıları kapalıyken gelmez",
                    prefs.notifyPlanB,
                    enabled = prefs.notifyWeather
                ) { v -> update { it.copy(notifyPlanB = v) } }
            }
        }
        Note("Hatırlatmalar Android'in iş zamanlayıcısıyla gelir; pil tasarrufunda birkaç dakika gecikebilir.")
    }
}

// endregion

// region Erişilebilirlik

@Composable
internal fun AccessibilityPage(
    prefs: ProfilePrefs,
    hooks: ProfileHooks,
    onBack: () -> Unit,
    update: ((ProfilePrefs) -> ProfilePrefs) -> Unit
) {
    val context = LocalContext.current
    val screenReader = remember {
        (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).isTouchExplorationEnabled
    }
    SubPage("Erişilebilirlik", onBack) {
        SoftCard {
            Column {
                SwitchRow("Büyük yazı", "Android'in yazı boyutunun üstüne %20 daha büyük", prefs.largeText) { v ->
                    update { it.copy(largeText = v) }
                }
                RowDivider()
                SwitchRow("Yüksek kontrast", "Açıklama metinleri daha koyu", prefs.highContrast) { v ->
                    update { it.copy(highContrast = v) }
                }
                RowDivider()
                SwitchRow("Animasyonları azalt", "Hareketli sahneler ve HavaCan durur", prefs.reduceMotion) { v ->
                    update { it.copy(reduceMotion = v) }
                }
            }
        }
        SoftCard {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Ekran okuyucu (TalkBack): ${if (screenReader) "açık" else "kapalı"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hava.ink,
                    fontWeight = FontWeight.Bold
                )
                Note("TalkBack ve Android'in yazı boyutu telefon ayarlarından açılıp kapanır; HavaCan bunları değiştiremez.")
                OutlinedButton(
                    onClick = { context.openSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("Android erişilebilirlik ayarlarını aç") }
            }
        }
        SectionTitle("Sesli rehber")
        val v = hooks.voice
        SoftCard {
            Column {
                SwitchRow(
                    stringResource(R.string.voice_setting_title),
                    stringResource(R.string.voice_setting_body),
                    v.settings.guideEnabled,
                    enabled = v.recognitionAvailable || v.settings.guideEnabled,
                    onChange = hooks.onGuideChange
                )
                listOfNotNull(
                    stringResource(R.string.voice_no_recognizer).takeIf { !v.recognitionAvailable },
                    stringResource(R.string.voice_no_turkish_tts).takeIf { v.tts == TtsStatus.NoTurkish },
                    stringResource(R.string.voice_no_tts).takeIf { v.tts == TtsStatus.Unavailable }
                ).forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RiskRed,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                    )
                }
                if (v.settings.guideEnabled) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.voice_howto_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Hava.ink,
                            fontWeight = FontWeight.Bold
                        )
                        Note(stringResource(R.string.voice_howto_body))
                        if (v.screenReader) Note(stringResource(R.string.voice_talkback_note))
                    }
                }
                RowDivider()
                SwitchRow(
                    stringResource(R.string.voice_speak_typed_title),
                    stringResource(R.string.voice_speak_typed_body),
                    v.settings.speakTypedReplies,
                    onChange = hooks.onSpeakTypedChange
                )
            }
        }
    }
}

// endregion

// region Gizlilik ve veriler

private enum class Wipe(val title: String, val body: String) {
    Chat("Sohbeti temizle", "HavaCan ile yazışmaların silinir."),
    Plans(
        "Planlarımı sil",
        "Tüm planların ve hatırlatmaları silinir. Takvimden gelen planlar takvim izni açıksa yeniden görünür. Telefon takvimine eklediğin etkinlikler silinmez."
    ),
    Wardrobe("Dolabımı sil", "Tüm kıyafetlerin, fotoğrafları ve kayıtlı kombinlerin silinir."),
    Profile("Profil tercihlerini sıfırla", "Adın, birim, aktivite, kombin, bildirim ve erişilebilirlik tercihlerin varsayılana döner.")
}

@Composable
internal fun PrivacyPage(
    stats: LocalStats,
    calendar: CalendarStatus,
    hooks: ProfileHooks,
    onBack: () -> Unit,
    viewModel: ProfileViewModel
) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<Wipe?>(null) }
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == android.content.pm.PackageManager.PERMISSION_GRANTED
    SubPage("Gizlilik ve veriler", onBack) {
        SectionTitle("Bu telefonda saklananlar")
        SoftCard {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Note("• Planlar: ${stats.plans}")
                Note("• Dolabım: ${stats.clothes} kıyafet (fotoğraflarıyla), ${stats.outfits} kayıtlı kombin")
                Note("• Seçili şehir, profil ve ses tercihlerin")
                Note("• Sohbet: yalnızca uygulama açıkken bellekte; kapatınca silinir")
            }
        }
        SectionTitle("Telefondan çıkanlar")
        SoftCard {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Note("• Open-Meteo: hava tahmini için şehrin koordinatı")
                Note(
                    "• HavaCan sunucusu ve OpenRouter (yapay zekâ): uygulamanın kendi kuralıyla anlamadığı cümleler, ana şehir ve yaklaşan plan adları/saatleri; kıyafet analizini istersen o fotoğraf"
                )
                Note(
                    "• Konuşma tanıma: sesin telefonunun konuşma tanıma hizmetince (ör. Google) yazıya çevrilir; HavaCan ses kaydı saklamaz"
                )
            }
        }
        SectionTitle("İzinler ve bağlı hesaplar")
        SoftCard {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Note("• Mikrofon: ${if (hasMicrophonePermission(context)) "izin var" else "izin yok"}")
                Note(
                    "• Takvim (Google Takvim dahil): ${if (calendar is CalendarStatus.Connected) "bağlı, yalnızca okuma" else "bağlı değil"}"
                )
                Note("• Bildirim: ${if (PlanNotifications.canNotify(context)) "izin var" else "izin yok"}")
                Note("• Kamera izni istenmez; fotoğrafı telefonun kamera uygulaması çeker")
                OutlinedButton(onClick = { context.appSettings() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("İzinleri Android'de yönet")
                }
            }
        }
        SectionTitle("Verilerini yönet")
        SoftCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Wipe.entries.forEach { w ->
                    OutlinedButton(onClick = { confirm = w }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(w.title, color = RiskRed)
                    }
                }
            }
        }
        SectionTitle(stringResource(R.string.about_title))
        Note(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
        Note(stringResource(R.string.about_weather_data))
        Note(stringResource(R.string.about_place_data))
        Note(stringResource(R.string.about_font))
    }
    confirm?.let { w ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("${w.title}?") },
            text = { Text(w.body + " Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    when (w) {
                        Wipe.Chat -> hooks.onClearChat()
                        Wipe.Plans -> viewModel.deleteAllPlans()
                        Wipe.Wardrobe -> viewModel.deleteWardrobe()
                        Wipe.Profile -> viewModel.resetProfile()
                    }
                    confirm = null
                }) { Text("Sil", color = RiskRed) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Vazgeç") } }
        )
    }
}

// endregion
