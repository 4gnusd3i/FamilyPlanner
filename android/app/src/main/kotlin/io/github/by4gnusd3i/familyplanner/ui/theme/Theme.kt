package io.github.by4gnusd3i.familyplanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val FamilyPlannerColors = lightColorScheme(
    primary = Color(0xFFFFAFCC),
    onPrimary = Color(0xFF22314D),
    secondary = Color(0xFFA2D2FF),
    onSecondary = Color(0xFF22314D),
    tertiary = Color(0xFFCDB4DB),
    onTertiary = Color(0xFF22314D),
    background = Color(0xFFFFF7FB),
    onBackground = Color(0xFF22314D),
    surface = Color(0xFFFFFDFF),
    onSurface = Color(0xFF22314D),
    surfaceVariant = Color(0xFFEFF8FF),
    onSurfaceVariant = Color(0xFF5C6680),
    outline = Color(0xFFD8E3F3),
    error = Color(0xFF9F3F48),
    onError = Color(0xFFFFFFFF),
)

private val FamilyPlannerTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
fun FamilyPlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FamilyPlannerColors,
        typography = FamilyPlannerTypography,
        content = content,
    )
}
