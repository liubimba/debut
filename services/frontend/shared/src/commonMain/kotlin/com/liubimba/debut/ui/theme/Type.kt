package com.liubimba.debut.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.space_grotesk_medium
import debut.shared.generated.resources.space_grotesk_regular
import debut.shared.generated.resources.space_grotesk_semibold
import org.jetbrains.compose.resources.Font

private const val TABULAR_FIGURES = "tnum"

@Composable
fun debutDisplayFamily(): FontFamily = FontFamily(
    Font(Res.font.space_grotesk_regular, FontWeight.Normal),
    Font(Res.font.space_grotesk_medium, FontWeight.Medium),
    Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
)

@Composable
fun debutTypography(): Typography {
    val display = debutDisplayFamily()
    val body = FontFamily.Default
    return Typography(
        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 56.sp,
            lineHeight = 56.sp,
        ),
        displayMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 41.4.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 35.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 41.4.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 35.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 29.7.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 29.7.sp,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        titleMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            lineHeight = 27.2.sp,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        titleSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 22.5.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 27.2.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.5.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.2.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 22.5.sp,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        labelMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.2.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.4.sp,
            letterSpacing = 0.09.em,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
    )
}
