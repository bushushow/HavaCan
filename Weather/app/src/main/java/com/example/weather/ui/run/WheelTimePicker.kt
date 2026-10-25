package com.example.weather.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.Typography
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.ui.theme.HavaBlueLight
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.NavySoft
import com.example.weather.ui.theme.Nunito
import com.example.weather.ui.theme.RiskRed
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.abs

private val ItemHeight = 48.dp
private const val VISIBLE = 5
private const val LOOPS = 400

/**
 * A clock-app time picker: hour and minute wheels that scroll and snap, or typed digits after
 * tapping the keyboard button. [note] explains the picked time (e.g. "Koşudan 30 dk önce");
 * [validate] returns an error to show and blocks confirming, or null when the time is fine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelTimeDialog(
    title: String,
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    note: (LocalTime) -> String? = { null },
    validate: (LocalTime) -> String? = { null },
) {
    var typing by rememberSaveable { mutableStateOf(false) }
    var hour by rememberSaveable { mutableIntStateOf(initial.hour) }
    var minute by rememberSaveable { mutableIntStateOf(initial.minute) }
    val typed = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    val picked = if (typing) LocalTime.of(typed.hour, typed.minute) else LocalTime.of(hour, minute)
    val error = validate(picked)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    // Carry the value across so switching modes never loses it
                    if (typing) { hour = typed.hour; minute = typed.minute } else { typed.hour = hour; typed.minute = minute }
                    typing = !typing
                }) {
                    Icon(
                        painterResource(if (typing) R.drawable.ic_schedule else R.drawable.ic_keyboard),
                        stringResource(if (typing) R.string.time_wheel else R.string.time_type),
                        tint = NavySoft,
                    )
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (typing) {
                    // The app's display styles are the hero sizes; the input needs the standard ones
                    MaterialTheme(typography = MaterialTheme.typography.copy(displayLarge = Typography().displayLarge.copy(fontFamily = Nunito), displayMedium = Typography().displayMedium.copy(fontFamily = Nunito))) {
                        TimeInput(typed)
                    }
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxWidth().height(ItemHeight).clip(RoundedCornerShape(14.dp)).background(HavaBlueLight))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Wheel(count = 24, value = hour, onValue = { hour = it }, description = stringResource(R.string.time_hour))
                            Text(":", style = MaterialTheme.typography.headlineMedium, color = Navy, modifier = Modifier.padding(horizontal = 8.dp))
                            Wheel(count = 60, value = minute, onValue = { minute = it }, description = stringResource(R.string.time_minute))
                        }
                    }
                }
                val text = error ?: note(picked)
                if (text != null) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (error != null) RiskRed else NavySoft, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(picked) }, enabled = error == null) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * "How long" as two wheels, hours and minutes in 5-minute steps, e.g. 0 sa 30 dk. Used inline
 * for the run's length and in [WheelDurationDialog] for the reminder.
 */
@Composable
fun DurationWheels(minutes: Long, maxHours: Int, visibleCount: Int, onMinutes: (Long) -> Unit) {
    val hours = (minutes / 60).toInt().coerceIn(0, maxHours)
    val step = ((minutes % 60) / 5).toInt()
    // Read the latest total inside the callbacks, not the one captured when the wheel was built
    val current by rememberUpdatedState(minutes)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.width(150.dp).height(ItemHeight).clip(RoundedCornerShape(12.dp)).background(HavaBlueLight))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Wheel(count = maxHours + 1, value = hours, onValue = { h -> onMinutes(h * 60L + current % 60) }, description = stringResource(R.string.time_hour), visibleCount = visibleCount, width = 44.dp, loop = false)
                Text("sa", style = MaterialTheme.typography.labelLarge, color = NavySoft)
                Wheel(count = 12, value = step, onValue = { m -> onMinutes(current / 60 * 60 + m * 5L) }, description = stringResource(R.string.time_minute), label = { "%02d".format(it * 5) }, visibleCount = visibleCount, width = 48.dp)
                Text("dk", style = MaterialTheme.typography.labelLarge, color = NavySoft)
            }
        }
    }
}

/** A dialog around [DurationWheels]; [validate] blocks confirming and says why. */
@Composable
fun WheelDurationDialog(
    title: String,
    initialMinutes: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
    note: (Long) -> String? = { null },
    validate: (Long) -> String? = { null },
) {
    var minutes by rememberSaveable { mutableLongStateOf(initialMinutes) }
    val error = validate(minutes)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DurationWheels(minutes, maxHours = 23, visibleCount = VISIBLE, onMinutes = { minutes = it })
                val text = error ?: note(minutes)
                if (text != null) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (error != null) RiskRed else NavySoft, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(minutes) }, enabled = error == null) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * One wheel of [count] rows; the centred row is the value. It loops unless [loop] is false, and
 * follows [value] when it changes from outside (e.g. an edited plan loading).
 */
@Composable
private fun Wheel(
    count: Int,
    value: Int,
    onValue: (Int) -> Unit,
    description: String,
    label: (Int) -> String = { "%02d".format(it) },
    visibleCount: Int = VISIBLE,
    width: androidx.compose.ui.unit.Dp = 72.dp,
    loop: Boolean = true,
) {
    val total = if (loop) count * LOOPS else count
    val base = if (loop) count * (LOOPS / 2) else 0
    val pad = visibleCount / 2
    val state = rememberLazyListState(initialFirstVisibleItemIndex = base + value)
    val fling = rememberSnapFlingBehavior(state, SnapPosition.Center)
    val scope = rememberCoroutineScope()
    val centre by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index?.minus(pad) ?: (base + value)
        }
    }
    val latest by rememberUpdatedState(onValue)
    LaunchedEffect(state) {
        snapshotFlow { centre }.collect { latest(it.coerceIn(0, total - 1) % count) }
    }
    // Follow outside changes without fighting the user's own scrolling
    LaunchedEffect(value) {
        if (!state.isScrollInProgress && centre % count != value) {
            state.scrollToItem(centre - centre % count + value)
        }
    }
    LazyColumn(
        state = state,
        flingBehavior = fling,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(width).height(ItemHeight * visibleCount).semantics { contentDescription = "$description ${label(value)}" },
    ) {
        // Blank rows above and below so the first and last values can reach the centre
        items(pad) { Box(Modifier.height(ItemHeight)) }
        items(total) { index ->
            val distance = abs(index - centre)
            Box(
                Modifier
                    .height(ItemHeight)
                    .fillMaxWidth()
                    .clickable { scope.launch { state.animateScrollToItem(index) } },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(index % count),
                    style = if (distance == 0) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    color = when (distance) {
                        0 -> Navy
                        1 -> NavySoft
                        else -> NavySoft.copy(alpha = 0.4f)
                    },
                )
            }
        }
        items(pad) { Box(Modifier.height(ItemHeight)) }
    }
}
