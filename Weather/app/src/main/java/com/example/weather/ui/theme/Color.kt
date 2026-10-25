package com.example.weather.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.weather.data.Region

// HavaCan palette: warm ivory canvas, deep navy type, vivid blue, sunny yellow
val Ivory = Color(0xFFFBF7EE)
val IvoryDeep = Color(0xFFF3EEE2)
val Navy = Color(0xFF0B1B3F)
val NavySoft = Color(0xFF4A5675)
val HavaBlue = Color(0xFF1D63F0)
val HavaBlueDeep = Color(0xFF0F47C2)
val HavaBlueLight = Color(0xFFE6EEFF)
val SkyTint = Color(0xFFF2F6FF)
val Sun = Color(0xFFFFC72C)
val SunDeep = Color(0xFFF5A300)
val SunLight = Color(0xFFFFF4CC)
val GoodGreen = Color(0xFF6CC24A)
val RiskRed = Color(0xFFE5484D)

val NightBackground = Color(0xFF0B1430)
val NightSurface = Color(0xFF15214A)
val NightSurfaceVariant = Color(0xFF1E2C5C)
val NightOnSurface = Color(0xFFEAF0FF)
val NightOnSurfaceVariant = Color(0xFFAEB9D8)

// Header gradient of the city search screen, top to bottom
val DayHeaderGradient = listOf(Color(0xFF3D82FF), HavaBlueDeep)
val NightHeaderGradient = listOf(Color(0xFF233A7A), Color(0xFF111C40))

// Plate badge colours per geographic region; dark enough for white text in both themes
val ForeignCityColor = Color(0xFF546E7A)

fun regionColor(region: Region): Color = when (region) {
    Region.MARMARA -> Color(0xFF1565C0)
    Region.EGE -> Color(0xFF00838F)
    Region.AKDENIZ -> Color(0xFFD84315)
    Region.IC_ANADOLU -> Color(0xFF8D6E00)
    Region.KARADENIZ -> Color(0xFF2E7D32)
    Region.DOGU_ANADOLU -> Color(0xFF6A1B9A)
    Region.GUNEYDOGU_ANADOLU -> Color(0xFFAD1457)
}
