package com.kampplus.havacan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kampplus.havacan.R

@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: FontWeight) = Font(
    resId = R.font.nunito,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/** Nunito (SIL OFL 1.1, see docs/licenses): rounded, friendly, very legible at large sizes. */
val Nunito = FontFamily(
    nunito(FontWeight.Normal),
    nunito(FontWeight.Medium),
    nunito(FontWeight.SemiBold),
    nunito(FontWeight.Bold),
    nunito(FontWeight.ExtraBold),
    nunito(FontWeight.Black)
)

private val Base = Typography()

private fun TextStyle.nunito(weight: FontWeight) = copy(fontFamily = Nunito, fontWeight = weight)

val Typography = Typography(
    displayLarge = Base.displayLarge.nunito(FontWeight.Black).copy(fontSize = 96.sp, lineHeight = 96.sp, letterSpacing = (-3).sp),
    displayMedium = Base.displayMedium.nunito(FontWeight.Black),
    displaySmall = Base.displaySmall.nunito(FontWeight.Black).copy(letterSpacing = (-1).sp),
    headlineLarge = Base.headlineLarge.nunito(FontWeight.ExtraBold),
    headlineMedium = Base.headlineMedium.nunito(FontWeight.ExtraBold),
    headlineSmall = Base.headlineSmall.nunito(FontWeight.ExtraBold),
    titleLarge = Base.titleLarge.nunito(FontWeight.ExtraBold),
    titleMedium = Base.titleMedium.nunito(FontWeight.Bold),
    titleSmall = Base.titleSmall.nunito(FontWeight.Bold),
    bodyLarge = Base.bodyLarge.nunito(FontWeight.SemiBold),
    bodyMedium = Base.bodyMedium.nunito(FontWeight.SemiBold),
    bodySmall = Base.bodySmall.nunito(FontWeight.SemiBold),
    labelLarge = Base.labelLarge.nunito(FontWeight.Bold),
    labelMedium = Base.labelMedium.nunito(FontWeight.Bold),
    labelSmall = Base.labelSmall.nunito(FontWeight.Bold)
)
