package com.kampplus.havacan.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kampplus.havacan.R
import com.kampplus.havacan.model.WeatherCondition
import com.kampplus.havacan.ui.home.character.drawHavaCanFace
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.SunLight

enum class HomeTab { Weather, Plans, HavaCan, Wardrobe, Profile }

/** Bottom navigation with HavaCan's face raised in the middle. */
@Composable
fun HavaCanBottomNavigation(selected: HomeTab, havaCanPulse: Int, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(12.dp, shape, ambientColor = Navy.copy(alpha = 0.12f), spotColor = Navy.copy(alpha = 0.12f))
                .background(Color.White, shape)
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 6.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            NavItem(HomeTab.Weather, selected, R.string.nav_weather, onSelect) { tint ->
                WeatherIcon(WeatherCondition.PartlyCloudy, isDay = true, size = 28.dp)
            }
            NavItem(HomeTab.Plans, selected, R.string.nav_plans, onSelect) { tint ->
                Icon(painterResource(R.drawable.ic_calendar), null, tint = tint, modifier = Modifier.size(26.dp))
            }
            HavaCanButton(havaCanPulse, selected == HomeTab.HavaCan) { onSelect(HomeTab.HavaCan) }
            NavItem(HomeTab.Wardrobe, selected, R.string.nav_wardrobe, onSelect) { tint ->
                Icon(painterResource(R.drawable.ic_hanger), null, tint = tint, modifier = Modifier.size(28.dp))
            }
            NavItem(HomeTab.Profile, selected, R.string.nav_profile, onSelect) { tint ->
                Icon(painterResource(R.drawable.ic_person), null, tint = tint, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun NavItem(tab: HomeTab, selected: HomeTab, label: Int, onSelect: (HomeTab) -> Unit, icon: @Composable (Color) -> Unit) {
    val isSelected = tab == selected
    val tint = if (isSelected) HavaBlue else NavySoft
    Column(
        Modifier
            .heightIn(min = 56.dp)
            .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) { icon(tint) }
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = tint)
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .width(18.dp)
                .height(3.dp)
                .background(if (isSelected) HavaBlue else Color.Transparent, CircleShape)
        )
    }
}

@Composable
private fun HavaCanButton(pulse: Int, isSelected: Boolean, onClick: () -> Unit) {
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(pulse) {
        if (pulse == 0) return@LaunchedEffect
        bounce.snapTo(0.82f)
        bounce.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 400f))
    }
    Column(
        Modifier
            .offset(y = (-6).dp)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(62.dp)
                .scale(bounce.value)
                .background(Brush.radialGradient(listOf(SunLight, Color.White.copy(alpha = 0f))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(50.dp)) { drawHavaCanFace(Offset(size.width / 2, size.height / 2), size.minDimension / 2.1f) }
        }
        Text(stringResource(R.string.nav_havacan), style = MaterialTheme.typography.labelMedium, color = if (isSelected) HavaBlue else Navy)
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .width(18.dp)
                .height(3.dp)
                .background(if (isSelected) HavaBlue else Color.Transparent, CircleShape)
        )
    }
}
