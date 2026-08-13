package com.liubimba.debut.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class DebutColors(
    val accentText: Color,
    val errorText: Color,
    val note: Color,
    val noteActive: Color,
    val signal: Color,
    val onSignal: Color,
    val good: Color,
    val chassis: Color,
    val onChassis: Color,
    val scrim: Color,
)

val DebutLightExtraColors = DebutColors(
    accentText = Color(0xFF4A4938),
    errorText = Color(0xFF9C3013),
    note = Color(0xFF7D7B6B),
    noteActive = Color(0xFF2B2A21),
    signal = Color(0xFFDFE93E),
    onSignal = Color(0xFF232219),
    good = Color(0xFF567F3B),
    chassis = Color(0xFF2C2B23),
    onChassis = Color(0xFFEFEEE4),
    scrim = Color(0x73232219),
)

val DebutDarkExtraColors = DebutColors(
    accentText = Color(0xFFD8E156),
    errorText = Color(0xFFF2A48C),
    note = Color(0xFF8A8974),
    noteActive = Color(0xFFF0EFE5),
    signal = Color(0xFFDBE44B),
    onSignal = Color(0xFF22221B),
    good = Color(0xFFA9C65F),
    chassis = Color(0xFF16160F),
    onChassis = Color(0xFFE9E8DB),
    scrim = Color(0xA6000000),
)

val DebutLightColorScheme = lightColorScheme(
    primary = Color(0xFF2F2E26),
    onPrimary = Color(0xFFF3F2EB),
    background = Color(0xFFE8E6DD),
    onBackground = Color(0xFF232219),
    surface = Color(0xFFF3F2EB),
    onSurface = Color(0xFF232219),
    surfaceVariant = Color(0xFFDEDBD0),
    onSurfaceVariant = Color(0xff5c5b49),
    outline = Color(0xFFCBC8BA),
    outlineVariant = Color(0xFFCBC8BA),
    error = Color(0xFFA83A20),
    onError = Color(0xFFF7F6F0),
    scrim = Color(0xFF232219),
)

val DebutDarkColorScheme = darkColorScheme(
    primary = Color(0xFFDBE44B),
    onPrimary = Color(0xFF22221B),
    background = Color(0xFF20201A),
    onBackground = Color(0xFFEEEDE3),
    surface = Color(0xFF2A2A23),
    onSurface = Color(0xFFEEEDE3),
    surfaceVariant = Color(0xFF35352C),
    onSurfaceVariant = Color(0xffaaa896),
    outline = Color(0xFF46453A),
    outlineVariant = Color(0xFF46453A),
    error = Color(0xFFE26A4D),
    onError = Color(0xFF241109),
    scrim = Color(0xFF000000),
)
