package com.kampplus.havacan.ui.plans

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.Typography
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kampplus.havacan.R
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.PlanSource
import com.kampplus.havacan.model.PlanType
import com.kampplus.havacan.model.Reminder
import com.kampplus.havacan.model.WeatherCondition
import com.kampplus.havacan.ui.home.character.HavaCanArt
import com.kampplus.havacan.ui.home.character.HavaCanPoseStyle
import com.kampplus.havacan.ui.home.components.SoftCard
import com.kampplus.havacan.ui.home.components.WeatherIcon
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueLight
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.Nunito
import com.kampplus.havacan.ui.theme.RiskRed
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr")
private val DateLabel = DateTimeFormatter.ofPattern("d MMMM yyyy", TURKISH)

/**
 * Create or edit a plan. The location is a city or district picked with the Kamp+ city search
 * ([onPickCity]); the free-text place name is only shown, never geocoded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    initial: Plan,
    isNew: Boolean,
    city: City,
    clock: () -> Float,
    onPickCity: () -> Unit,
    onSave: (plan: Plan, addToCalendar: Boolean) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    now: () -> LocalDateTime = LocalDateTime::now
) {
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var date by rememberSaveable { mutableStateOf(initial.date) }
    var time by rememberSaveable { mutableStateOf(initial.time) }
    var place by rememberSaveable { mutableStateOf(initial.placeName.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(initial.type) }
    var follow by rememberSaveable { mutableStateOf(initial.follow) }
    var reminder by rememberSaveable { mutableStateOf(initial.reminder) }
    var addToCalendar by rememberSaveable { mutableStateOf(false) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showReminders by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back), tint = Navy) }
                Text(
                    stringResource(if (isNew) R.string.new_plan else R.string.edit_plan),
                    style = MaterialTheme.typography.titleLarge,
                    color = Navy
                )
            }
            HavaCanArt(HavaCanPoseStyle.Peek, clock, Modifier.align(Alignment.TopEnd).size(110.dp, 90.dp))
        }
        Column(Modifier.padding(start = 4.dp)) {
            Text(
                stringResource(R.string.new_plan_headline),
                style = MaterialTheme.typography.headlineLarge,
                color = Navy,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.new_plan_sub), style = MaterialTheme.typography.bodyLarge, color = NavySoft)
        }

        FormField(stringResource(R.string.field_title)) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    error = null
                },
                placeholder = { Text(stringResource(R.string.field_title_hint)) },
                trailingIcon = { Icon(painterResource(R.drawable.ic_edit), null, tint = NavySoft) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = plainFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PickerField(
                stringResource(R.string.field_date),
                LocalDate.parse(date).format(DateLabel),
                R.drawable.ic_calendar,
                Modifier.weight(1.3f)
            ) {
                showDate =
                    true
            }
            PickerField(stringResource(R.string.field_time), time.replace(':', '.'), R.drawable.ic_schedule, Modifier.weight(1f)) {
                showTime =
                    true
            }
        }
        PickerField(
            stringResource(R.string.field_city),
            listOfNotNull(
                city.name,
                city.admin1?.takeIf {
                    it != city.name
                }
            ).joinToString(", "),
            R.drawable.ic_place,
            Modifier.fillMaxWidth(),
            onPickCity
        )
        FormField(stringResource(R.string.field_place)) {
            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                placeholder = { Text(stringResource(R.string.field_place_hint)) },
                singleLine = true,
                colors = plainFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            stringResource(R.string.field_type),
            style = MaterialTheme.typography.titleMedium,
            color = NavySoft,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlanType.entries.forEach { option ->
                val selected = option == type
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) HavaBlueLight else Color.White)
                        .border(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) HavaBlue else Navy.copy(alpha = 0.1f),
                            RoundedCornerShape(18.dp)
                        )
                        .selectable(selected = selected, role = Role.RadioButton) { type = option }
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    when (option) {
                        PlanType.Outdoor -> WeatherIcon(WeatherCondition.Clear, true, size = 22.dp)
                        PlanType.Indoor -> Icon(
                            painterResource(R.drawable.ic_home),
                            null,
                            tint = if (selected) HavaBlue else NavySoft,
                            modifier = Modifier.size(22.dp)
                        )
                        PlanType.Travel -> Icon(
                            painterResource(R.drawable.ic_car),
                            null,
                            tint = if (selected) HavaBlue else NavySoft,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        option.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) HavaBlue else NavySoft,
                        maxLines = 1
                    )
                }
            }
        }

        SoftCard(background = androidx.compose.ui.graphics.SolidColor(HavaBlueLight)) {
            Row(
                Modifier
                    .toggleable(value = follow, role = Role.Switch) { follow = it }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeatherIcon(WeatherCondition.PartlyCloudy, true, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.follow_title), style = MaterialTheme.typography.titleMedium, color = Navy)
                    Text(stringResource(R.string.follow_body), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                }
                Switch(checked = follow, onCheckedChange = null)
            }
        }

        Box {
            SoftCard {
                Row(
                    Modifier.clickable(role = Role.Button) { showReminders = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_bell), null, tint = HavaBlue)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.reminder), style = MaterialTheme.typography.bodySmall, color = NavySoft)
                        Text(reminder.label, style = MaterialTheme.typography.titleMedium, color = Navy)
                    }
                    Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Navy)
                }
            }
            DropdownMenu(expanded = showReminders, onDismissRequest = { showReminders = false }) {
                Reminder.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = {
                        reminder = option
                        showReminders = false
                    })
                }
            }
        }

        if (isNew && initial.source == PlanSource.App) {
            SoftCard {
                Row(
                    Modifier.toggleable(value = addToCalendar, role = Role.Checkbox) { addToCalendar = it }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(HavaBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_calendar), null, tint = HavaBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.add_to_calendar), style = MaterialTheme.typography.titleMedium, color = Navy)
                        Text(stringResource(R.string.add_to_calendar_body), style = MaterialTheme.typography.bodySmall, color = NavySoft)
                    }
                    Checkbox(checked = addToCalendar, onCheckedChange = null)
                }
            }
        }

        error?.let {
            Text(
                stringResource(it),
                color = RiskRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Button(
            onClick = {
                val dateTime = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time))
                error = when {
                    title.isBlank() -> R.string.error_title_required
                    dateTime.isBefore(now()) -> R.string.error_past
                    else -> null
                }
                if (error == null) {
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            date = date,
                            time = time,
                            city = city,
                            placeName = place.trim().ifBlank { null },
                            type = type,
                            follow = follow,
                            reminder = reminder,
                            lastNotified = null
                        ),
                        addToCalendar
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Icon(painterResource(R.drawable.ic_check), null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.save_plan), style = MaterialTheme.typography.titleMedium)
        }
        if (!isNew) {
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.delete_plan), color = RiskRed)
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showDate) {
        val today = now().toLocalDate()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDate = false
                    error = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(pickerState, title = null) }
    }
    if (showTime) {
        val current = LocalTime.parse(time)
        val timeState = rememberTimePickerState(current.hour, current.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    time = "%02d:%02d".format(timeState.hour, timeState.minute)
                    showTime = false
                    error = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.cancel)) } },
            // The app's displayLarge is the 96 sp hero temperature; the picker needs the standard size
            text = {
                MaterialTheme(
                    typography = MaterialTheme.typography.copy(displayLarge = Typography().displayLarge.copy(fontFamily = Nunito))
                ) {
                    TimePicker(timeState)
                }
            }
        )
    }
}

@Composable
private fun FormField(label: String, content: @Composable () -> Unit) {
    SoftCard {
        Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = NavySoft, modifier = Modifier.padding(start = 12.dp))
            content()
        }
    }
}

@Composable
private fun PickerField(label: String, value: String, icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier) {
        SoftCard {
            Row(
                Modifier.clickable(role = Role.Button, onClick = onClick).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = NavySoft)
                    Text(value, style = MaterialTheme.typography.titleMedium, color = Navy, maxLines = 1)
                }
                Icon(painterResource(icon), null, tint = HavaBlue)
            }
        }
    }
}

@Composable
private fun plainFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)
