package com.example.weather.ui.wardrobe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.data.wardrobe.ClothingCategory
import com.example.weather.data.wardrobe.ClothingItem
import com.example.weather.domain.wardrobe.OutfitResult
import com.example.weather.domain.wardrobe.WeatherContext
import com.example.weather.model.WeatherCondition
import com.example.weather.ui.home.character.HavaCanArt
import com.example.weather.ui.home.character.HavaCanPoseStyle
import com.example.weather.ui.home.components.HavaCanLogo
import com.example.weather.ui.home.components.SoftCard
import com.example.weather.ui.home.components.WeatherIcon
import com.example.weather.ui.theme.HavaBlue
import com.example.weather.ui.theme.HavaBlueLight
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.NavySoft
import com.example.weather.ui.theme.Sun
import kotlin.math.roundToInt

/**
 * "Bugünün kombini": pieces from the user's own wardrobe for today's weather. Shaking the phone
 * or tapping "Tekrar salla" calls the same function; each new outfit replays a short entrance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutfitScreen(
    outfit: OutfitState,
    weather: WeatherContext?,
    clock: () -> Float,
    onShakeAgain: () -> Unit,
    onSave: () -> Unit,
    onAddCategory: (ClothingCategory) -> Unit,
    onBack: () -> Unit,
) {
    // HavaCan hops and the cards drop in on every new outfit
    val hop = remember { Animatable(0f) }
    LaunchedEffect(outfit.generation) {
        hop.snapTo(1f)
        hop.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFBF0), Color(0xFFEFF5FF))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackLink(HavaBlue, HavaBlue, onBack)
            Spacer(Modifier.weight(1f))
            WeatherChip(weather)
        }

        Box(Modifier.fillMaxWidth().height(170.dp)) {
            Column(Modifier.align(Alignment.CenterStart).padding(start = 4.dp, end = 150.dp)) {
                Text(
                    stringResource(R.string.todays_outfit),
                    style = MaterialTheme.typography.displaySmall,
                    color = Navy,
                    modifier = Modifier
                        .semantics { heading() }
                        .drawBehind {
                            drawLine(Sun, Offset(0f, size.height + 4f), Offset(size.width * 0.7f, size.height), 5.dp.toPx(), StrokeCap.Round)
                        },
                )
                Spacer(Modifier.height(10.dp))
                if (outfit.result is OutfitResult.Ready) {
                    Text(stringResource(R.string.from_your_wardrobe), style = MaterialTheme.typography.bodyLarge, color = NavySoft)
                }
            }
            HavaCanArt(
                HavaCanPoseStyle.Cheer,
                clock,
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(150.dp)
                    .graphicsLayer {
                        translationY = -hop.value * 40.dp.toPx()
                        rotationZ = hop.value * -12f
                    },
            )
        }

        when (val result = outfit.result) {
            is OutfitResult.Ready -> ReadyOutfit(result, outfit.generation)
            is OutfitResult.Missing -> MissingPieces(result.categories, onAddCategory)
            null -> Unit
        }

        Button(
            onClick = onShakeAgain,
            colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        ) {
            Icon(painterResource(R.drawable.ic_shake), null)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(if (outfit.result is OutfitResult.Ready) R.string.shake_again else R.string.shake_try), style = MaterialTheme.typography.titleLarge)
        }
        if (outfit.result is OutfitResult.Ready) {
            OutlinedButton(
                onClick = onSave,
                enabled = !outfit.saved,
                border = BorderStroke(1.5.dp, HavaBlue),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(painterResource(if (outfit.saved) R.drawable.ic_check_circle else R.drawable.ic_bookmark), null, tint = HavaBlue)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(if (outfit.saved) R.string.outfit_saved else R.string.save_outfit), style = MaterialTheme.typography.titleMedium, color = HavaBlue)
            }
        }
        Text(
            stringResource(R.string.shake_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = NavySoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WeatherChip(weather: WeatherContext?) {
    Row(
        Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (weather != null) {
            WeatherIcon(if (weather.rainLikely) WeatherCondition.Rain else WeatherCondition.Clear, true, size = 22.dp)
            Spacer(Modifier.width(6.dp))
            Text("${weather.placeName} · ${weather.feelsLike.roundToInt()}°", style = MaterialTheme.typography.labelLarge, color = Navy)
        } else {
            Text(stringResource(R.string.no_weather), style = MaterialTheme.typography.labelLarge, color = NavySoft)
        }
    }
}

@Composable
private fun ReadyOutfit(result: OutfitResult.Ready, generation: Int) {
    SoftCard(background = Brush.verticalGradient(listOf(Color(0xFFF4F8FF), Color(0xFFDCE8FF)))) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_hanger), null, tint = HavaBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pieces_from_wardrobe, result.items.size), style = MaterialTheme.typography.titleSmall, color = HavaBlue)
            }
            Spacer(Modifier.height(12.dp))
            // Two rows of at most three, so four pieces (with a coat) still fit
            result.items.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item ->
                        val index = result.items.indexOf(item)
                        PieceCard(item, index, generation, Modifier.weight(1f))
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    SoftCard(background = Brush.horizontalGradient(listOf(HavaBlueLight, Color(0xFFF6F9FF)))) {
        Row(Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(WeatherCondition.PartlyCloudy, true, size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.why_this), style = MaterialTheme.typography.titleMedium, color = Navy)
                Text(result.reason, style = MaterialTheme.typography.bodyLarge, color = NavySoft)
                result.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = HavaBlue, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
    }
}

@Composable
private fun PieceCard(item: ClothingItem, index: Int, generation: Int, modifier: Modifier) {
    val enter = remember(generation) { Animatable(0f) }
    LaunchedEffect(generation) {
        kotlinx.coroutines.delay(90L * index)
        enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }
    Column(
        modifier
            .graphicsLayer {
                alpha = enter.value.coerceIn(0f, 1f)
                translationY = (1 - enter.value) * 60.dp.toPx()
                rotationZ = (1 - enter.value) * if (index % 2 == 0) -8f else 8f
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LocalImage(item.photoPath, item.name, Modifier.fillMaxWidth().aspectRatio(0.85f), maxSide = 420)
        Text(
            item.name,
            style = MaterialTheme.typography.titleSmall,
            color = Navy,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        )
    }
}

/** Not enough clothes: say exactly what to add, never show an invented outfit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissingPieces(categories: List<ClothingCategory>, onAdd: (ClothingCategory) -> Unit) {
    SoftCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.missing_title), style = MaterialTheme.typography.titleLarge, color = Navy)
            Text(stringResource(R.string.missing_body), style = MaterialTheme.typography.bodyLarge, color = NavySoft)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { c ->
                    OutlinedButton(onClick = { onAdd(c) }, border = BorderStroke(1.5.dp, HavaBlue)) {
                        Icon(painterResource(R.drawable.ic_add), null, tint = HavaBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(c.label, color = HavaBlue)
                    }
                }
            }
        }
    }
}
