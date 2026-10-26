package com.kampplus.havacan.ui.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kampplus.havacan.R
import com.kampplus.havacan.model.ClothingCategory
import com.kampplus.havacan.model.ClothingItem
import com.kampplus.havacan.ui.common.OnSky
import com.kampplus.havacan.ui.common.OnSkySoft
import com.kampplus.havacan.ui.common.SkyHeader
import com.kampplus.havacan.ui.home.character.HavaCanArt
import com.kampplus.havacan.ui.home.character.HavaCanPoseStyle
import com.kampplus.havacan.ui.home.components.HavaCanLogo
import com.kampplus.havacan.ui.home.components.SoftCard
import com.kampplus.havacan.ui.theme.Hava
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.SunLight

/** The wardrobe: every piece the user added, filters, saved outfits and the way into Salla Kombin. */
@Composable
fun WardrobeScreen(
    state: WardrobeUiState,
    totalCount: Int,
    clock: () -> Float,
    onAdd: () -> Unit,
    onOpenItem: (ClothingItem) -> Unit,
    onFilter: (ClothingCategory?) -> Unit,
    onMakeOutfit: () -> Unit,
    onDeleteOutfit: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkyHeader(bleed = 16.dp) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                HavaCanLogo(Modifier.padding(start = 4.dp))
            }
            Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.nav_wardrobe),
                    style = MaterialTheme.typography.displaySmall,
                    color = OnSky,
                    modifier = Modifier.weight(1f).semantics {
                        heading()
                    }
                )
                Text(
                    stringResource(R.string.wardrobe_count, totalCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSkySoft,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        SallaCard(clock, onMakeOutfit)

        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Icon(painterResource(R.drawable.ic_add), null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_clothing), style = MaterialTheme.typography.titleSmall)
        }

        if (totalCount > 0) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(stringResource(R.string.filter_all), state.filter == null) { onFilter(null) }
                ClothingCategory.entries.forEach { c -> FilterChip(c.label, state.filter == c) { onFilter(c) } }
            }
        }

        when {
            totalCount == 0 -> EmptyWardrobe(clock)
            state.items.isEmpty() -> Text(
                stringResource(R.string.filter_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = Hava.inkSoft
            )
            else -> state.items.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { item -> ItemCard(item, Modifier.weight(1f)) { onOpenItem(item) } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (state.savedOutfits.isNotEmpty()) {
            Text(
                stringResource(R.string.saved_outfits),
                style = MaterialTheme.typography.titleLarge,
                color = Hava.ink,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp).semantics {
                    heading()
                }
            )
            state.savedOutfits.forEach { saved ->
                SoftCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        saved.items.take(4).forEach { item ->
                            LocalImage(item.photoPath, item.name, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)), maxSide = 200)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            saved.outfit.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Hava.inkSoft,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                        IconButton(onClick = { onDeleteOutfit(saved.outfit.id) }) {
                            Icon(painterResource(R.drawable.ic_clear), stringResource(R.string.delete_outfit), tint = Hava.inkSoft)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun SallaCard(clock: () -> Float, onClick: () -> Unit) {
    SoftCard(background = Hava.p.sallaCard) {
        Row(
            Modifier.clickable(role = Role.Button, onClick = onClick).padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.salla_title), style = MaterialTheme.typography.headlineSmall, color = Hava.ink)
                Text(stringResource(R.string.salla_hint), style = MaterialTheme.typography.bodyMedium, color = Hava.inkSoft)
            }
            HavaCanArt(HavaCanPoseStyle.Cheer, clock, Modifier.size(96.dp))
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.White else Hava.inkSoft,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) HavaBlue else Hava.card)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun ItemCard(item: ClothingItem, modifier: Modifier, onClick: () -> Unit) {
    SoftCard(modifier) {
        Column(Modifier.clickable(role = Role.Button, onClick = onClick)) {
            LocalImage(item.photoPath, item.name, Modifier.fillMaxWidth().aspectRatio(1f), maxSide = 480)
            Column(Modifier.padding(12.dp)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Hava.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.category.label} · ${item.colorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hava.inkSoft,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyWardrobe(clock: () -> Float) {
    SoftCard {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wardrobe_empty_title), style = MaterialTheme.typography.titleLarge, color = Hava.ink)
                Text(stringResource(R.string.wardrobe_empty_body), style = MaterialTheme.typography.bodyLarge, color = Hava.inkSoft)
            }
            HavaCanArt(HavaCanPoseStyle.Cheer, clock, Modifier.size(90.dp))
        }
    }
}
