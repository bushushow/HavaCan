package com.example.weather.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

val Typography = Typography(
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.3.sp
    ),
)
