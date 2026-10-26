package com.kampplus.havacan.ui.meet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kampplus.havacan.R
import com.kampplus.havacan.domain.meet.MeetOption
import com.kampplus.havacan.domain.meet.MeetOutcome
import com.kampplus.havacan.domain.meet.PersonWeather
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.MeetPerson
import com.kampplus.havacan.model.MeetWindow
import com.kampplus.havacan.model.deg
import com.kampplus.havacan.ui.common.DayStrip
import com.kampplus.havacan.ui.home.character.HavaCanArt
import com.kampplus.havacan.ui.home.character.HavaCanPoseStyle
import com.kampplus.havacan.ui.home.components.SoftCard
import com.kampplus.havacan.ui.home.components.WeatherIcon
import com.kampplus.havacan.ui.plans.capitalizeTr
import com.kampplus.havacan.ui.theme.GoodGreen
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueLight
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.RiskRed
import com.kampplus.havacan.ui.theme.SkyTint
import com.kampplus.havacan.ui.theme.Sun
import com.kampplus.havacan.ui.theme.SunLight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr")
private val OptionDay = DateTimeFormatter.ofPattern("d MMMM EEEE", TURKISH)
private val AvatarColors = listOf(Color(0xFF1D63F0), Color(0xFFF5A300), Color(0xFF2E9E5B), Color(0xFFE5484D), Color(0xFF7B5CD6))

class MeetActions(
    val onBack: () -> Unit,
    val onTitle: (String) -> Unit,
    val onAddPerson: (String, City) -> Unit,
    val onRemovePerson: (Int) -> Unit,
    val onPickCity: (onPicked: (City) -> Unit) -> Unit,
    val onDate: (LocalDate) -> Unit,
    val onToggleWindow: (MeetWindow) -> Unit,
    val onPlace: (City?) -> Unit,
    val onOption: (Int) -> Unit,
    val onRetry: () -> Unit,
    val onSave: () -> Unit,
    val onShare: () -> Unit,
    val onDone: () -> Unit
)

/**
 * Weather Meet in three steps on one page: who's coming, when, and HavaCan's pick with
 * everyone's weather. Every number on it comes from the Open-Meteo forecast.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetScreen(state: MeetUiState, clock: () -> Float, actions: MeetActions) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back), tint = Navy)
            }
            Text(stringResource(R.string.meet_title), style = MaterialTheme.typography.titleLarge, color = Navy)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    stringResource(R.string.meet_headline),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Navy,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.meet_sub), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
            }
            HavaCanArt(HavaCanPoseStyle.Cheer, clock, Modifier.size(96.dp))
        }

        Step(1, stringResource(R.string.meet_step_people))
        PeopleCard(state, actions)

        Step(2, stringResource(R.string.meet_step_when))
        DayStrip(
            title = stringResource(R.string.meet_day),
            today = state.today,
            selected = state.date,
            conditions = state.people.firstOrNull()?.city
                ?.let { state.forecasts[MeetViewModel.key(it)] }?.daily.orEmpty()
                .associate { it.date to it.condition },
            onDate = actions.onDate,
            onPickOther = { showDate = true }
        )
        WindowRow(state.windows, actions.onToggleWindow)
        PlaceRow(state, actions)

        Step(3, stringResource(R.string.meet_step_pick))
        Result(state, actions)

        Spacer(Modifier.height(12.dp))
    }

    if (showDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(state.today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { actions.onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    showDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(pickerState, title = null) }
    }
}

@Composable
private fun Step(number: Int, title: String) {
    Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(HavaBlue), contentAlignment = Alignment.Center) {
            Text("$number", style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Navy, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun Avatar(name: String, index: Int) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(AvatarColors[index % AvatarColors.size]),
        contentAlignment = Alignment.Center
    ) {
        Text(name.take(1).uppercase(TURKISH), style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

private fun City.label() = listOfNotNull(name, admin1?.takeIf { it != name }).joinToString(", ")

@Composable
private fun PeopleCard(state: MeetUiState, actions: MeetActions) {
    var name by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable(stateSaver = CitySaver) { mutableStateOf<City?>(null) }
    SoftCard {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.people.forEachIndexed { i, person ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(person.name, i)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(person.name, style = MaterialTheme.typography.titleMedium, color = Navy)
                        Text(
                            person.city.label(),
                            style = MaterialTheme.typography.bodySmall,
                            color = NavySoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (i > 0) {
                        IconButton(onClick = { actions.onRemovePerson(i) }) {
                            Icon(painterResource(R.drawable.ic_clear), stringResource(R.string.meet_remove, person.name), tint = NavySoft)
                        }
                    }
                }
            }

            // Add someone: a name and where they'll come from
            Box(Modifier.fillMaxWidth().height(1.dp).background(Navy.copy(alpha = 0.06f)))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                placeholder = { Text(stringResource(R.string.meet_name_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { actions.onPickCity { city = it } },
                    border = BorderStroke(1.dp, Navy.copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_place), null, tint = HavaBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        city?.label() ?: stringResource(R.string.meet_pick_place),
                        color = if (city == null) NavySoft else Navy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = {
                        city?.let { actions.onAddPerson(name, it) }
                        name = ""
                        city = null
                    },
                    enabled = name.isNotBlank() && city != null,
                    colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.meet_add))
                }
            }
        }
    }
}

@Composable
private fun WindowRow(windows: Set<MeetWindow>, onToggle: (MeetWindow) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MeetWindow.entries.forEach { window ->
            val on = window in windows
            val bg by animateColorAsState(if (on) HavaBlue else Color.White, tween(200), label = "window")
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bg)
                    .border(1.dp, if (on) Color.Transparent else Navy.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                    .toggleable(on, role = Role.Checkbox) { onToggle(window) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(window.label, style = MaterialTheme.typography.titleSmall, color = if (on) Color.White else Navy, maxLines = 1)
                Text(
                    "%02d–%02d".format(window.hours.first, window.hours.last + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) Color.White.copy(alpha = 0.85f) else NavySoft
                )
            }
        }
    }
}

@Composable
private fun PlaceRow(state: MeetUiState, actions: MeetActions) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        SoftCard {
            Row(
                Modifier.clickable(role = Role.Button) { open = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_place), null, tint = HavaBlue, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.meet_place), style = MaterialTheme.typography.bodySmall, color = NavySoft)
                    Text(
                        state.place?.label() ?: stringResource(R.string.meet_place_auto),
                        style = MaterialTheme.typography.titleMedium,
                        color = Navy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(painterResource(R.drawable.ic_expand_more), null, tint = Navy)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.meet_place_auto)) }, onClick = {
                actions.onPlace(null)
                open = false
            })
            state.people.map { it.city }.distinctBy { it.latitude to it.longitude }.forEach { city ->
                DropdownMenuItem(text = { Text(city.label()) }, onClick = {
                    actions.onPlace(city)
                    open = false
                })
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.meet_place_other)) },
                onClick = {
                    open = false
                    actions.onPickCity { actions.onPlace(it) }
                }
            )
        }
    }
}

@Composable
private fun Result(state: MeetUiState, actions: MeetActions) {
    if (state.failed.isNotEmpty()) {
        Notice(
            stringResource(R.string.meet_failed, state.failed.joinToString(", ")),
            action = stringResource(R.string.run_retry) to actions.onRetry
        )
    }
    val outcome = state.outcome
    when {
        state.loading && state.options.isEmpty() -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HavaBlue)
        }
        outcome == MeetOutcome.NeedPeople -> Notice(stringResource(R.string.meet_need_people))
        outcome is MeetOutcome.NoForecastYet -> Notice(stringResource(R.string.meet_no_forecast, outcome.daysUntilAvailable.toInt()))
        outcome == MeetOutcome.NoHoursLeft -> Notice(stringResource(R.string.meet_no_hours))
        state.options.isEmpty() -> Notice(stringResource(R.string.meet_no_options))
        else -> {
            val chosen = state.chosen ?: return
            AnimatedContent(chosen, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "pick") { option ->
                PickCard(option)
            }
            if (state.options.size > 1) {
                Text(
                    stringResource(R.string.meet_other_options),
                    style = MaterialTheme.typography.titleSmall,
                    color = NavySoft,
                    modifier = Modifier.padding(start = 4.dp)
                )
                state.options.forEachIndexed { i, option ->
                    if (i != state.selected) {
                        OptionRow(option, isPlanB = option == state.planB) { actions.onOption(i) }
                    }
                }
            }
            Actions(state, actions)
        }
    }
}

@Composable
private fun PickCard(option: MeetOption) {
    SoftCard(background = Brush.linearGradient(listOf(Color(0xFFE8F0FF), Color.White))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val (text, bg, fg) = if (option.everyoneDry) {
                Triple(stringResource(R.string.meet_all_good), Color(0xFFEAF7E4), Color(0xFF2E7D32))
            } else {
                Triple(option.risk?.label ?: stringResource(R.string.meet_someone_wet), SunLight, Color(0xFF7A5200))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 5.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(option.time.format(OptionDay).capitalizeTr(), style = MaterialTheme.typography.bodyLarge, color = NavySoft)
                    Text("%02d.00".format(option.time.hour), style = MaterialTheme.typography.displaySmall, color = Navy)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_place), null, tint = HavaBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(option.place.name, style = MaterialTheme.typography.titleMedium, color = Navy)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WeatherIcon(option.hour.condition, option.hour.isDay, size = 56.dp)
                    Text("${option.hour.temperature.deg()}°", style = MaterialTheme.typography.headlineSmall, color = Navy)
                    Text(
                        option.hour.precipitationProbability?.let { stringResource(R.string.meet_rain, it) } ?: option.hour.condition.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = NavySoft
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Navy.copy(alpha = 0.06f)))
            Text(stringResource(R.string.meet_everyone), style = MaterialTheme.typography.titleSmall, color = NavySoft)
            option.people.forEachIndexed { i, p -> PersonLine(p, i) }
        }
    }
}

@Composable
private fun PersonLine(p: PersonWeather, index: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(AvatarColors[index % AvatarColors.size]))
        Spacer(Modifier.width(8.dp))
        Text(
            "${p.person.name} · ${p.person.city.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = Navy,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val h = p.hour
        when {
            h == null -> Text(stringResource(R.string.meet_unknown), style = MaterialTheme.typography.bodySmall, color = NavySoft)
            else -> {
                WeatherIcon(h.condition, h.isDay, size = 22.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    "${h.temperature.deg()}°" + (p.risk?.let { " · ${it.noun}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (p.risk != null) RiskRed else NavySoft
                )
            }
        }
    }
}

@Composable
private fun OptionRow(option: MeetOption, isPlanB: Boolean, onClick: () -> Unit) {
    SoftCard {
        Row(
            Modifier.selectable(selected = false, role = Role.RadioButton, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("%02d.00".format(option.time.hour), style = MaterialTheme.typography.titleMedium, color = Navy)
                    if (isPlanB) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.meet_plan_b),
                            style = MaterialTheme.typography.labelMedium,
                            color = HavaBlue,
                            modifier = Modifier.clip(CircleShape).background(HavaBlueLight).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    option.place.name + (option.risk?.let { " · ${it.label}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (option.risk != null) RiskRed else NavySoft
                )
            }
            WeatherIcon(option.hour.condition, option.hour.isDay, size = 30.dp)
            Spacer(Modifier.width(6.dp))
            Text("${option.hour.temperature.deg()}°", style = MaterialTheme.typography.titleMedium, color = Navy)
        }
    }
}

@Composable
private fun Actions(state: MeetUiState, actions: MeetActions) {
    val saved = state.saved
    if (saved == null) {
        OutlinedTextField(
            value = state.title,
            onValueChange = actions.onTitle,
            label = { Text(stringResource(R.string.meet_name_label)) },
            placeholder = { Text(MeetViewModel.defaultTitle(state.people)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = actions.onSave,
            enabled = !state.saving,
            colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)
        ) {
            if (state.saving) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(painterResource(R.drawable.ic_calendar), null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.meet_save), style = MaterialTheme.typography.titleMedium)
            }
        }
        OutlinedButton(
            onClick = actions.onShare,
            border = BorderStroke(1.5.dp, HavaBlue),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Text(stringResource(R.string.meet_share), color = HavaBlue, style = MaterialTheme.typography.titleSmall)
        }
    } else {
        SoftCard(background = Brush.linearGradient(listOf(Color(0xFFEAF7E4), Color.White))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Sun), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = Navy)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.meet_saved), style = MaterialTheme.typography.titleMedium, color = Navy)
                    Text(stringResource(R.string.meet_saved_body), style = MaterialTheme.typography.bodySmall, color = NavySoft)
                }
                Icon(painterResource(R.drawable.ic_check_circle), null, tint = GoodGreen)
            }
        }
        Button(
            onClick = actions.onShare,
            colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)
        ) { Text(stringResource(R.string.meet_share), style = MaterialTheme.typography.titleMedium) }
        TextButton(onClick = actions.onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.meet_back_to_plans), color = HavaBlue)
        }
    }
}

@Composable
private fun Notice(text: String, action: Pair<String, () -> Unit>? = null) {
    SoftCard(background = androidx.compose.ui.graphics.SolidColor(SkyTint)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Navy, modifier = Modifier.weight(1f))
            if (action != null) TextButton(onClick = action.second) { Text(action.first, color = HavaBlue) }
        }
    }
}

private val gson = com.google.gson.Gson()

private val CitySaver = androidx.compose.runtime.saveable.Saver<City?, String>(
    save = { it?.let(gson::toJson) ?: "" },
    restore = { if (it.isEmpty()) null else gson.fromJson(it, City::class.java) }
)
