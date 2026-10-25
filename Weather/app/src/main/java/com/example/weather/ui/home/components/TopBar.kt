package com.example.weather.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.ui.theme.HavaBlue
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.Sun

@Composable
fun HomeTopBar(
    cityName: String,
    runnerActive: Boolean,
    onCityClick: () -> Unit,
    onRunnerToggle: () -> Unit,
    onBellClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val bell = @Composable {
            IconButton(onClick = onBellClick) {
                Icon(painterResource(R.drawable.ic_bell), stringResource(R.string.cd_notifications), tint = Navy)
            }
        }
        if (maxWidth >= 400.dp) {
            Row(
                Modifier.padding(start = 16.dp, end = 0.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HavaCanLogo()
                Spacer(Modifier.weight(1f))
                LocationChip(cityName, onCityClick)
                RunnerModeToggle(runnerActive, onRunnerToggle)
                bell()
            }
        } else {
            // Narrow phones and large font sizes: controls drop to a second row instead of overflowing
            Column(Modifier.padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HavaCanLogo()
                    Spacer(Modifier.weight(1f))
                    bell()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocationChip(cityName, onCityClick)
                    RunnerModeToggle(runnerActive, onRunnerToggle)
                }
            }
        }
    }
}

/** "HavaCan" word mark with a sun arc over "Can". */
@Composable
fun HavaCanLogo(modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Bottom) {
        Text("Hava", style = MaterialTheme.typography.headlineSmall, color = Navy, maxLines = 1)
        Box(
            Modifier.drawBehind {
                drawArc(
                    color = Sun,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.12f, -size.height * 0.18f),
                    size = Size(size.width * 0.76f, size.height * 0.8f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                )
            },
        ) {
            Text("Can", style = MaterialTheme.typography.headlineSmall, color = Navy, maxLines = 1)
        }
    }
}

@Composable
private fun LocationChip(cityName: String, onClick: () -> Unit) {
    val description = stringResource(R.string.cd_choose_city, cityName)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Navy.copy(alpha = 0.05f),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .semantics { contentDescription = description },
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_place), null, tint = HavaBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                cityName,
                style = MaterialTheme.typography.labelLarge,
                color = Navy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 96.dp),
            )
            Icon(painterResource(R.drawable.ic_expand_more), null, tint = Navy, modifier = Modifier.size(18.dp))
        }
    }
}

/** Outlined when off, filled blue with a check when on; colours and the check animate. */
@Composable
fun RunnerModeToggle(active: Boolean, onToggle: () -> Unit) {
    val container by animateColorAsState(if (active) HavaBlue else Color.Transparent, spring(), label = "toggleBg")
    val content by animateColorAsState(if (active) Color.White else HavaBlue, spring(), label = "toggleFg")
    val description = stringResource(if (active) R.string.cd_runner_mode_on else R.string.cd_runner_mode_off)
    Surface(
        onClick = onToggle,
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = BorderStroke(1.5.dp, HavaBlue),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .semantics { contentDescription = description },
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The runner icon turns into a check when on, so the pill keeps its width
            AnimatedContent(
                targetState = active,
                transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "toggleIcon",
            ) { on ->
                Icon(
                    painterResource(if (on) R.drawable.ic_check else R.drawable.ic_run),
                    null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.runner_mode), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
