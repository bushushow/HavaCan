package com.kampplus.havacan.ui.profile

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.havacan.R
import com.kampplus.havacan.data.ProfilePrefs
import com.kampplus.havacan.ui.common.OnSky
import com.kampplus.havacan.ui.common.OnSkySoft
import com.kampplus.havacan.ui.common.SkyHeader
import com.kampplus.havacan.ui.home.character.HavaCanArt
import com.kampplus.havacan.ui.home.character.HavaCanPoseStyle
import com.kampplus.havacan.ui.home.components.SoftCard
import com.kampplus.havacan.ui.theme.Hava
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueLight
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.RiskRed
import com.kampplus.havacan.ui.theme.SkyTint
import com.kampplus.havacan.ui.voice.VoiceGuideState
import com.kampplus.havacan.ui.voice.VoicePhase

enum class ProfilePage { Main, CityUnits, Activities, Styles, Notifications, Accessibility, Privacy }

/** What Profil needs from the rest of the app; voice and city stay owned by the shell. */
class ProfileHooks(
    val cityName: String,
    val voice: VoiceGuideState,
    val onPickCity: () -> Unit,
    /** Turns the voice guide on if needed (asking for the microphone) and starts a session. */
    val onStartVoice: () -> Unit,
    val onStopVoice: () -> Unit,
    val onGuideChange: (Boolean) -> Unit,
    val onSpeakTypedChange: (Boolean) -> Unit,
    val onClearChat: () -> Unit
)

@Composable
fun ProfileRoute(hooks: ProfileHooks, clock: () -> Float, viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(ProfilePage.Main) }
    if (page != ProfilePage.Main) BackHandler { page = ProfilePage.Main }

    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::onCalendarResult)
    val connectCalendar = {
        viewModel.onCalendarRequested()
        calendarPermission.launch(Manifest.permission.READ_CALENDAR)
    }
    LaunchedEffect(Unit) { viewModel.refreshCalendar() }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState == ProfilePage.Main) {
                (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
            } else {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
            }
        },
        label = "profilePage"
    ) { current ->
        val back = { page = ProfilePage.Main }
        when (current) {
            ProfilePage.Main -> ProfileMain(prefs, hooks, calendar, clock, onOpen = {
                page = it
            }, onConnectCalendar = connectCalendar, onSaveName = { n -> viewModel.update { it.copy(name = n) } })
            ProfilePage.CityUnits -> CityUnitsPage(prefs, hooks, back) { u -> viewModel.update { it.copy(unit = u) } }
            ProfilePage.Activities -> ActivitiesPage(prefs, back, viewModel::update)
            ProfilePage.Styles -> StylesPage(prefs, back, viewModel::update)
            ProfilePage.Notifications -> NotificationsPage(prefs, back, viewModel::update)
            ProfilePage.Accessibility -> AccessibilityPage(prefs, hooks, back, viewModel::update)
            ProfilePage.Privacy -> PrivacyPage(stats, calendar, hooks, back, viewModel)
        }
    }
}

@Composable
private fun ProfileMain(
    prefs: ProfilePrefs,
    hooks: ProfileHooks,
    calendar: CalendarStatus,
    clock: () -> Float,
    onOpen: (ProfilePage) -> Unit,
    onConnectCalendar: () -> Unit,
    onSaveName: (String) -> Unit
) {
    var editName by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkyHeader(bleed = 20.dp) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Profilim",
                    style = MaterialTheme.typography.displaySmall,
                    color = OnSky,
                    modifier = Modifier.weight(1f).semantics {
                        heading()
                    }
                )
                IconButton(onClick = { onOpen(ProfilePage.Accessibility) }, modifier = Modifier.size(52.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_settings),
                        "Erişilebilirlik ve ses ayarları",
                        tint = OnSky,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        GreetingCard(prefs.name, hooks.cityName, clock, onEdit = { editName = true })

        SectionTitle("HavaCan deneyimin")
        VoiceGuideCard(hooks)

        SectionTitle("Bağlantılar")
        CalendarCard(calendar, onConnectCalendar)

        SectionTitle("Kişiselleştirme")
        SoftCard {
            Column {
                SettingsRow(R.drawable.ic_place, "Şehir ve birimler", "${hooks.cityName} · ${prefs.unit.symbol}") {
                    onOpen(ProfilePage.CityUnits)
                }
                RowDivider()
                SettingsRow(
                    R.drawable.ic_run,
                    "Aktivite tercihlerim",
                    prefs.activities.summary {
                        it.label
                    }
                ) { onOpen(ProfilePage.Activities) }
                RowDivider()
                SettingsRow(R.drawable.ic_hanger, "Kombin tercihlerim", prefs.styles.summary { it.label }) { onOpen(ProfilePage.Styles) }
            }
        }

        SectionTitle("Uygulama ayarları")
        SoftCard {
            Column {
                val on = listOfNotNull(
                    "hava".takeIf { prefs.notifyWeather },
                    "plan".takeIf { prefs.notifyPlans },
                    "koşu".takeIf { prefs.notifyRuns }
                )
                SettingsRow(R.drawable.ic_bell, "Bildirimler", if (on.isEmpty()) "Hepsi kapalı" else "Açık: ${on.joinToString(", ")}") {
                    onOpen(ProfilePage.Notifications)
                }
                RowDivider()
                SettingsRow(R.drawable.ic_accessibility, "Erişilebilirlik", "Ekran okuyucu, yazı boyutu, sesli rehber") {
                    onOpen(ProfilePage.Accessibility)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) {
                onOpen(ProfilePage.Privacy)
            }.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(painterResource(R.drawable.ic_lock), null, tint = HavaBlue, modifier = Modifier.size(24.dp))
            Text(
                "Gizlilik ve veriler",
                style = MaterialTheme.typography.titleMedium,
                color = HavaBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = HavaBlue)
        }
        Spacer(Modifier.height(110.dp))
    }
    if (editName) {
        NameDialog(prefs.name, onDismiss = { editName = false }, onSave = {
            onSaveName(it)
            editName = false
        })
    }
}

private fun <T> Set<T>.summary(label: (T) -> String): String = if (isEmpty()) {
    "Seçilmedi"
} else {
    map(label).sorted().let {
        if (it.size >
            2
        ) {
            it.take(2).joinToString(", ") + " +${it.size - 2}"
        } else {
            it.joinToString(", ")
        }
    }
}

@Composable
@Suppress("ktlint:standard:max-line-length")
private fun GreetingCard(name: String, city: String, clock: () -> Float, onEdit: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    val description = if (name.isBlank()) "Merhaba! Adını eklemek için dokun. Şehir: $city" else "Merhaba, $name! Şehir: $city. Adını düzenlemek için dokun."
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .background(Brush.linearGradient(listOf(Hava.card, Hava.cardTint, Hava.p.blueTint)), shape)
            .clickable(onClickLabel = "Adını düzenle", role = Role.Button, onClick = onEdit)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        // Soft clouds behind HavaCan, drawn rather than an image
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(size.width * 0.82f, size.height * 0.86f)
            listOf(Offset(-60f, 0f) to 46f, Offset(0f, -18f) to 58f, Offset(60f, 4f) to 44f).forEach { (o, r) ->
                val scale = density / 2.6f
                drawCircle(Color.White.copy(alpha = 0.85f), r * scale, c + o * scale)
            }
        }
        HavaCanArt(HavaCanPoseStyle.Cheer, clock, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp).size(124.dp))
        Column(
            Modifier.align(Alignment.CenterStart).padding(start = 24.dp, end = 130.dp, top = 22.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (name.isBlank()) "Merhaba! 👋" else "Merhaba, $name! 👋",
                style = MaterialTheme.typography.headlineSmall,
                color = Hava.ink
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_place), null, tint = HavaBlue, modifier = Modifier.size(22.dp))
                Text(city, style = MaterialTheme.typography.titleMedium, color = Hava.inkSoft)
            }
            if (name.isBlank()) {
                Text("Adını ekle", style = MaterialTheme.typography.labelLarge, color = HavaBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VoiceGuideCard(hooks: ProfileHooks) {
    val v = hooks.voice
    val active = v.sessionActive
    val status = when {
        !v.recognitionAvailable -> "Bu telefonda konuşma tanıma hizmeti yok; sesli rehber açılamıyor."
        active && v.phase == VoicePhase.Listening -> "Dinliyorum… Konuşabilirsin."
        active && v.phase == VoicePhase.Speaking -> "HavaCan konuşuyor…"
        active && v.phase == VoicePhase.Thinking -> "HavaCan düşünüyor…"
        active -> "Açık · konuşmak için mikrofona dokun"
        else -> "HavaCan'ı tüm uygulamada sesinle kullan"
    }
    Box(
        Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(Hava.p.blueTint, Hava.cardTint, Hava.card)),
            RoundedCornerShape(30.dp)
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(76.dp).background(Hava.card, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_headset), null, tint = HavaBlue, modifier = Modifier.size(44.dp))
                Canvas(Modifier.size(width = 24.dp, height = 18.dp)) {
                    for (i in 0..3) {
                        val h = size.height * (0.35f + 0.65f * (if (i % 2 == 0) 0.6f else 1f))
                        val x = size.width * (0.15f + i * 0.23f)
                        drawLine(
                            HavaBlue,
                            Offset(x, (size.height - h) / 2),
                            Offset(x, (size.height + h) / 2),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sesli Rehber", style = MaterialTheme.typography.titleLarge, color = Hava.ink, fontWeight = FontWeight.ExtraBold)
                Text(status, style = MaterialTheme.typography.bodyMedium, color = if (!v.recognitionAvailable) RiskRed else Hava.inkSoft)
                v.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = RiskRed) }
            }
            if (v.recognitionAvailable) {
                Surface(
                    onClick = if (active) hooks.onStopVoice else hooks.onStartVoice,
                    shape = CircleShape,
                    color = if (active) RiskRed else HavaBlue,
                    shadowElevation = 4.dp,
                    modifier = Modifier.heightIn(min = 52.dp).semantics {
                        contentDescription =
                            if (active) "Sesli rehberi durdur" else "Sesli rehberi başlat"
                    }
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (active) "Durdur" else "Başlat",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            painterResource(if (active) R.drawable.ic_stop else R.drawable.ic_play),
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(status: CalendarStatus, onConnect: () -> Unit) {
    val (line, button) = when (status) {
        CalendarStatus.NotConnected -> "Bağlı değil" to "Bağla"
        CalendarStatus.Connecting -> "Bağlanıyor…" to null
        is CalendarStatus.Connected -> (status.events?.let { "Bağlı · 14 günde $it etkinlik" } ?: "Bağlı · etkinlikler okunuyor…") to null
        is CalendarStatus.Error -> status.message to "Tekrar dene"
    }
    SoftCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(52.dp).background(Hava.p.blueTint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_calendar), null, tint = HavaBlue, modifier = Modifier.size(30.dp))
                }
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                    Text("Google Takvim", style = MaterialTheme.typography.titleMedium, color = Hava.ink, fontWeight = FontWeight.Bold)
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status is CalendarStatus.Error) RiskRed else Hava.inkSoft
                    )
                }
                if (button != null) {
                    Surface(
                        onClick = onConnect,
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, HavaBlue),
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Google Takvim'i $button" }
                    ) {
                        Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(button, style = MaterialTheme.typography.titleSmall, color = HavaBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Text(
                "Telefonundaki takvim hesapları (Google dahil) okunur; planların takvimde görünür. HavaCan takvimde hiçbir etkinliği kendisi değiştirmez.",
                style = MaterialTheme.typography.bodySmall,
                color = Hava.inkSoft
            )
        }
    }
}

@Composable
private fun NameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adın") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Yalnızca bu telefonda saklanır; HavaCan seni böyle selamlar.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(30) },
                    singleLine = true,
                    label = { Text("Ad") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Kaydet") } },
        dismissButton = {
            Row {
                if (current.isNotBlank()) TextButton(onClick = { onSave("") }) { Text("Adı sil", color = RiskRed) }
                TextButton(onClick = onDismiss) { Text("Vazgeç") }
            }
        }
    )
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = Hava.ink,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 6.dp).semantics {
            heading()
        }
    )
}

@Composable
internal fun SettingsRow(icon: Int, title: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(painterResource(icon), null, tint = HavaBlue, modifier = Modifier.size(32.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Hava.ink, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = Hava.inkSoft)
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Hava.ink)
    }
}

@Composable
internal fun RowDivider() = HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = Hava.ink.copy(alpha = 0.07f))
