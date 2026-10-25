package com.example.weather.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.weather.data.Region

// Brand palette: clear-sky blues
val SkyPrimary = Color(0xFF1E5AA8)
val SkyPrimaryContainer = Color(0xFFD8E6FF)
val SkyOnPrimaryContainer = Color(0xFF0B2A55)
val SkyBackground = Color(0xFFF3F6FB)
val SkySurfaceVariant = Color(0xFFE6ECF5)
val SkyOnSurface = Color(0xFF162033)
val SkyOnSurfaceVariant = Color(0xFF55627A)
val SkyOutline = Color(0xFFC4CEDD)

val NightPrimary = Color(0xFF9EC3FF)
val NightOnPrimary = Color(0xFF0B2A55)
val NightPrimaryContainer = Color(0xFF203E6B)
val NightOnPrimaryContainer = Color(0xFFD8E6FF)
val NightBackground = Color(0xFF0D131E)
val NightSurface = Color(0xFF172030)
val NightSurfaceVariant = Color(0xFF232D3F)
val NightOnSurface = Color(0xFFE4E9F2)
val NightOnSurfaceVariant = Color(0xFFADB8CA)
val NightOutline = Color(0xFF3A465B)

// Header gradient, top to bottom
val DayHeaderGradient = listOf(Color(0xFF5C9EEA), Color(0xFF2A5FB5))
val NightHeaderGradient = listOf(Color(0xFF253B63), Color(0xFF131D30))

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
