package com.mehmetdem.dil.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DilInk = Color(0xFF111A35)
val DilMuted = Color(0xFF6B7287)
val DilCanvas = Color(0xFFFCFCFD)
val DilPaper = Color(0xFFFFFFFF)
val DilTeal = Color(0xFF0789A3)
val DilTealDark = Color(0xFF05758C)
val DilBlueSoft = Color(0xFFEAF5FF)
val DilGreenSoft = Color(0xFFE8F8EF)
val DilRedSoft = Color(0xFFFFECEF)
val DilPurpleSoft = Color(0xFFF1ECFF)
val DilBorder = Color(0xFFE4E7EE)

private val Ink = DilInk
private val Canvas = DilCanvas
private val Paper = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = DilTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F7FB),
    onPrimaryContainer = Color(0xFF005268),
    secondary = Color(0xFF11A675),
    onSecondary = Color(0xFF06251E),
    secondaryContainer = Color(0xFFC4F4E6),
    tertiary = Color(0xFF6B4FD3),
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF4F6F9),
    outline = DilBorder,
)

private val DilTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

private val DilShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun DilTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = DilTypography,
        shapes = DilShapes,
        content = content,
    )
}
