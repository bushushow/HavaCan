package com.example.weather.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.weather.data.Region

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

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
