package com.kampplus.havacan.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kampplus.havacan.R
import com.kampplus.havacan.domain.plans.PlanOutlook
import com.kampplus.havacan.model.PlanSource
import com.kampplus.havacan.ui.home.character.HavaCanArt
import com.kampplus.havacan.ui.home.character.HavaCanPoseStyle
import com.kampplus.havacan.ui.home.components.SoftCard
import com.kampplus.havacan.ui.home.components.WeatherIcon
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueLight
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.SkyTint
import com.kampplus.havacan.ui.theme.Sun
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PlanBChoice { Move, Indoor, Keep }

private val PlanDate = DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr"))

/** Alternatives for a plan at weather risk. Nothing changes until the user confirms. */
@Composable
fun PlanBScreen(item: PlanItem, clock: () -> Float, onApply: (PlanBChoice) -> Unit, onBack: () -> Unit) {
    val plan = item.plan
    val alternative = item.alternative
    val risk = item.outlook as? PlanOutlook.Risk
    var choice by rememberSaveable { mutableStateOf(if (alternative != null) PlanBChoice.Move else PlanBChoice.Indoor) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val earlier = alternative != null && alternative.hour < plan.localTime.hour

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back), tint = Navy) }
            Text(stringResource(R.string.plan_b_crumb), style = MaterialTheme.typography.titleMedium, color = HavaBlue)
        }
        Column(Modifier.padding(start = 4.dp)) {
            Text(
                stringResource(R.string.plan_b_title),
                style = MaterialTheme.typography.displaySmall,
                color = Navy,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.plan_b_sub, risk?.reason?.noun ?: "Hava"),
                style = MaterialTheme.typography.titleLarge,
                color = NavySoft
            )
        }
        SoftCard(background = Brush.horizontalGradient(listOf(HavaBlueLight, Color(0xFFF4F8FF)))) {
            Row(Modifier.heightIn(min = 96.dp).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_lightbulb), null, tint = HavaBlue)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.plan_b_info),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Navy,
                    modifier = Modifier.weight(1f)
                )
                HavaCanArt(HavaCanPoseStyle.Umbrella, clock, Modifier.size(120.dp))
            }
        }
        SoftCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(plan.title, style = MaterialTheme.typography.headlineSmall, color = Navy)
                Text(
                    "${plan.localDate.format(PlanDate).capitalizeTr()} • ${plan.timeLabel}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NavySoft
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_place), null, tint = NavySoft, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        listOfNotNull(plan.placeName, plan.city.name).joinToString(", "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = NavySoft
                    )
                }
                OutlookChip(item)
            }
        }
        Column(Modifier.padding(start = 4.dp, top = 6.dp)) {
            Text(
                stringResource(R.string.plan_b_question),
                style = MaterialTheme.typography.headlineSmall,
                color = Navy,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            Text(stringResource(R.string.plan_b_question_sub), style = MaterialTheme.typography.bodyLarge, color = NavySoft)
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionCard(
                selected = choice == PlanBChoice.Move,
                enabled = alternative != null,
                onSelect = { choice = PlanBChoice.Move },
                icon = {
                    if (alternative != null) {
                        WeatherIcon(alternative.condition, alternative.isDay, size = 40.dp)
                    } else {
                        Icon(painterResource(R.drawable.ic_schedule), null, tint = NavySoft)
                    }
                },
                title = stringResource(if (earlier) R.string.option_earlier else R.string.option_later),
                recommended = alternative != null
            ) {
                if (alternative != null && risk != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeChip(plan.timeLabel, Color(0xFFFDE8E8))
                        Icon(painterResource(R.drawable.ic_arrow_forward), null, tint = NavySoft, modifier = Modifier.size(18.dp))
                        TimeChip(alternative.hourLabel, Color(0xFFEAF7E4))
                    }
                    Text(stringResource(R.string.option_time_body), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                } else {
                    Text(stringResource(R.string.option_no_time), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                }
            }
            OptionCard(
                selected = choice == PlanBChoice.Indoor,
                onSelect = { choice = PlanBChoice.Indoor },
                icon = { Icon(painterResource(R.drawable.ic_cafe), null, tint = Navy, modifier = Modifier.size(30.dp)) },
                title = stringResource(R.string.option_indoor)
            ) { Text(stringResource(R.string.option_indoor_body), style = MaterialTheme.typography.bodyMedium, color = NavySoft) }
            OptionCard(
                selected = choice == PlanBChoice.Keep,
                onSelect = { choice = PlanBChoice.Keep },
                icon = { Icon(painterResource(R.drawable.ic_bell), null, tint = Navy, modifier = Modifier.size(30.dp)) },
                title = stringResource(R.string.option_keep)
            ) { Text(stringResource(R.string.option_keep_body), style = MaterialTheme.typography.bodyMedium, color = NavySoft) }
        }
        if (plan.source == PlanSource.Calendar) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SkyTint).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_shield), null, tint = NavySoft, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.calendar_note), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
            }
        }
        Button(
            onClick = { confirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.review_choice), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(6.dp))
            Icon(painterResource(R.drawable.ic_chevron_right), null)
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.go_back), style = MaterialTheme.typography.titleSmall, color = HavaBlue)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirm) {
        val body = when (choice) {
            PlanBChoice.Move -> stringResource(R.string.confirm_time, plan.title, plan.timeLabel, alternative?.hourLabel.orEmpty())
            PlanBChoice.Indoor -> stringResource(R.string.confirm_indoor, plan.title)
            PlanBChoice.Keep -> stringResource(R.string.confirm_keep, plan.title)
        }
        val calendarNote = if (plan.source == PlanSource.Calendar &&
            choice == PlanBChoice.Move
        ) {
            "\n\n" + stringResource(R.string.confirm_calendar)
        } else {
            ""
        }
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.confirm_title)) },
            text = { Text(body + calendarNote) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onApply(choice)
                }) { Text(stringResource(R.string.apply)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun OptionCard(
    selected: Boolean,
    onSelect: () -> Unit,
    icon: @Composable () -> Unit,
    title: String,
    enabled: Boolean = true,
    recommended: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) HavaBlueLight else Color.White)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) HavaBlue else Navy.copy(alpha = 0.08f), shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(SkyTint), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) Navy else NavySoft)
                    if (recommended) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.recommended),
                            style = MaterialTheme.typography.labelSmall,
                            color = Navy,
                            modifier = Modifier.background(Sun, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
private fun TimeChip(text: String, background: Color) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = Navy,
        modifier = Modifier.background(SolidColor(background), CircleShape).padding(horizontal = 14.dp, vertical = 6.dp)
    )
}
