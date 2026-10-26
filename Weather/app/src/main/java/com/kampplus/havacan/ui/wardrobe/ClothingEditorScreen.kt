package com.kampplus.havacan.ui.wardrobe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.kampplus.havacan.domain.wardrobe.ClothingColors
import com.kampplus.havacan.model.ClothingCategory
import com.kampplus.havacan.model.Season
import com.kampplus.havacan.ui.common.OnSky
import com.kampplus.havacan.ui.common.OnSkySoft
import com.kampplus.havacan.ui.common.SkyHeader
import com.kampplus.havacan.ui.home.character.HavaCanArt
import com.kampplus.havacan.ui.home.character.HavaCanPoseStyle
import com.kampplus.havacan.ui.home.components.SoftCard
import com.kampplus.havacan.ui.theme.GoodGreen
import com.kampplus.havacan.ui.theme.Hava
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueLight
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.RiskRed
import com.kampplus.havacan.ui.theme.SkyTint
import com.kampplus.havacan.ui.theme.Sun

/** Add or edit a piece: photo (camera or gallery), AI or manual labels, save to Room. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClothingEditorScreen(
    state: EditorState,
    aiConfigured: Boolean,
    clock: () -> Float,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onName: (String) -> Unit,
    onCategory: (ClothingCategory) -> Unit,
    onColor: (String) -> Unit,
    onToggleSeason: (Season) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkyHeader(bleed = 16.dp) {
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                Row(Modifier.align(Alignment.TopStart).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackLink(OnSky, OnSkySoft, onBack)
                    Spacer(Modifier.width(24.dp))
                    Text(
                        stringResource(if (state.isNew) R.string.add_clothing else R.string.edit_clothing),
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSky
                    )
                }
                HavaCanArt(HavaCanPoseStyle.Cheer, clock, Modifier.align(Alignment.BottomEnd).size(110.dp))
                Column(Modifier.align(Alignment.BottomStart).padding(start = 4.dp, end = 110.dp)) {
                    Text(
                        stringResource(R.string.add_clothing_headline),
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSky,
                        modifier = Modifier.semantics {
                            heading()
                        }
                    )
                    Text(stringResource(R.string.add_clothing_sub), style = MaterialTheme.typography.bodyMedium, color = OnSkySoft)
                }
            }
        }
        PhotoArea(state, onPickPhoto)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onTakePhoto,
                colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)
            ) {
                Icon(painterResource(R.drawable.ic_camera), null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.take_photo), style = MaterialTheme.typography.titleSmall)
            }
            OutlinedButton(
                onClick = onPickPhoto,
                border = BorderStroke(1.5.dp, HavaBlue),
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)
            ) {
                Icon(painterResource(R.drawable.ic_gallery), null, tint = HavaBlue)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pick_photo), style = MaterialTheme.typography.titleSmall, color = HavaBlue)
            }
        }
        state.photoError?.let { Text(it, color = RiskRed, style = MaterialTheme.typography.bodyMedium) }

        SoftCard {
            Column {
                AnalysisHeader(state, aiConfigured)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.field_piece_name),
                            style = MaterialTheme.typography.titleSmall,
                            color = Hava.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text(stringResource(R.string.edit_yourself), style = MaterialTheme.typography.bodySmall, color = Hava.inkSoft)
                    }
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onName,
                        placeholder = { Text(stringResource(R.string.field_piece_hint)) },
                        trailingIcon = {
                            if (state.name.isNotEmpty()) {
                                IconButton(onClick = {
                                    onName("")
                                }) { Icon(painterResource(R.drawable.ic_clear), stringResource(R.string.search_clear)) }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1.1f)) {
                            Text(stringResource(R.string.field_category), style = MaterialTheme.typography.titleSmall, color = Hava.ink)
                            Spacer(Modifier.height(6.dp))
                            Dropdown(
                                value = state.category?.label ?: stringResource(R.string.choose),
                                leading = {
                                    Icon(painterResource(R.drawable.ic_hanger), null, tint = Hava.ink, modifier = Modifier.size(22.dp))
                                },
                                options = ClothingCategory.entries.map { it.label },
                                onSelect = { onCategory(ClothingCategory.entries[it]) }
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.field_color), style = MaterialTheme.typography.titleSmall, color = Hava.ink)
                            Spacer(Modifier.height(6.dp))
                            val swatch = state.colorName?.let { ClothingColors.byName(it) }
                            Dropdown(
                                value = state.colorName ?: stringResource(R.string.choose),
                                leading = { Swatch(swatch?.rgb) },
                                options = ClothingColors.all.map { it.name },
                                optionLeading = { i -> Swatch(ClothingColors.all[i].rgb) },
                                onSelect = { onColor(ClothingColors.all[it].name) }
                            )
                        }
                    }
                    Text(stringResource(R.string.field_season), style = MaterialTheme.typography.titleSmall, color = Hava.ink)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Season.entries.forEach { season ->
                            val on = season in state.seasons
                            Row(
                                Modifier
                                    .clip(CircleShape)
                                    .background(if (on) Hava.p.blueTint else Hava.card)
                                    .border(1.dp, if (on) HavaBlue else Hava.ink.copy(alpha = 0.12f), CircleShape)
                                    .toggleable(value = on, role = Role.Checkbox) { onToggleSeason(season) }
                                    .heightIn(min = 44.dp)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (on) {
                                    Icon(
                                        painterResource(R.drawable.ic_check_circle),
                                        null,
                                        tint = HavaBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(season.label, style = MaterialTheme.typography.labelLarge, color = if (on) HavaBlue else Hava.inkSoft)
                            }
                        }
                    }
                    Text(stringResource(R.string.season_hint), style = MaterialTheme.typography.bodySmall, color = Hava.inkSoft)
                }
            }
        }

        state.error?.let { Text(it, color = RiskRed, style = MaterialTheme.typography.bodyMedium) }
        Button(
            onClick = onSave,
            enabled = state.canSave,
            colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
        ) {
            Icon(painterResource(R.drawable.ic_check_circle), null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.save_to_wardrobe), style = MaterialTheme.typography.titleMedium)
        }
        if (!state.canSave && state.photoPath == null) {
            Text(
                stringResource(R.string.photo_required),
                style = MaterialTheme.typography.bodySmall,
                color = Hava.inkSoft,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        if (!state.isNew) {
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.delete_clothing), color = RiskRed)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PhotoArea(state: EditorState, onPick: () -> Unit) {
    SoftCard {
        Box(Modifier.fillMaxWidth().height(320.dp).background(Hava.cardTint)) {
            val path = state.photoPath
            if (path != null) {
                LocalImage(path, state.name.ifBlank { null }, Modifier.fillMaxSize(), maxSide = 1080)
                Row(
                    Modifier.padding(14.dp).clip(CircleShape).background(Color(0xFFE6F6E0)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_check_circle), null, tint = GoodGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.photo_selected), style = MaterialTheme.typography.labelLarge, color = Color(0xFF2E7D32))
                }
                IconButton(
                    onClick = onPick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(52.dp).clip(CircleShape).background(Hava.card)
                ) { Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.change_photo), tint = Hava.ink) }
            } else {
                Column(
                    Modifier.align(Alignment.Center).clickable(role = Role.Button, onClick = onPick).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(painterResource(R.drawable.ic_gallery), null, tint = Hava.inkSoft, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.no_photo_yet), style = MaterialTheme.typography.bodyLarge, color = Hava.inkSoft)
                }
            }
            if (state.importing) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = HavaBlue)
            }
        }
    }
}

@Composable
private fun AnalysisHeader(state: EditorState, aiConfigured: Boolean) {
    val (title, body) = when (state.analysis) {
        AnalysisState.Idle -> stringResource(R.string.ai_idle_title) to
            stringResource(if (aiConfigured) R.string.ai_idle_body else R.string.ai_off_body)
        AnalysisState.Analyzing -> stringResource(R.string.ai_analyzing_title) to stringResource(R.string.ai_analyzing_body)
        AnalysisState.Recognized -> stringResource(R.string.ai_done_title) to stringResource(R.string.ai_done_body)
        AnalysisState.Failed -> stringResource(R.string.ai_failed_title) to stringResource(R.string.ai_failed_body)
        AnalysisState.NotConfigured -> stringResource(R.string.ai_off_title) to stringResource(R.string.ai_off_body)
    }
    val pulse by rememberInfiniteTransition(
        label = "ai"
    ).animateFloat(0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    Row(
        Modifier.fillMaxWidth().background(
            Hava.p.highlight
        ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Hava.card), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_sparkle),
                null,
                tint = Sun,
                modifier = Modifier.size(28.dp).alpha(if (state.analysis == AnalysisState.Analyzing) pulse else 1f)
            )
        }
        Spacer(Modifier.width(12.dp))
        AnimatedContent(targetState = title to body, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "aiText") { (t, b) ->
            Column {
                Text(t, style = MaterialTheme.typography.titleMedium, color = Hava.ink)
                Text(b, style = MaterialTheme.typography.bodyMedium, color = Hava.inkSoft)
            }
        }
    }
}

@Composable
private fun Dropdown(
    value: String,
    leading: @Composable () -> Unit,
    options: List<String>,
    onSelect: (Int) -> Unit,
    optionLeading: (@Composable (Int) -> Unit)? = null
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Hava.ink.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                .selectable(selected = open, role = Role.DropdownList) { open = true }
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Spacer(Modifier.width(10.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = Hava.ink, modifier = Modifier.weight(1f), maxLines = 1)
            Icon(painterResource(R.drawable.ic_expand_more), null, tint = Hava.inkSoft)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = optionLeading?.let { { it(i) } },
                    onClick = {
                        onSelect(i)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Swatch(rgb: Int?) {
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(rgb?.let { Color(0xFF000000 or it.toLong()) } ?: Color.Transparent)
            .border(1.dp, Hava.ink.copy(alpha = 0.15f), CircleShape)
    )
}

/** "← Dolabım": the arrow and the label are one touch target. */
@Composable
internal fun BackLink(arrowTint: Color, labelColor: Color, onBack: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onBack)
            .heightIn(min = 48.dp)
            .padding(start = 12.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back), tint = arrowTint)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.nav_wardrobe), style = MaterialTheme.typography.titleMedium, color = labelColor)
    }
}
