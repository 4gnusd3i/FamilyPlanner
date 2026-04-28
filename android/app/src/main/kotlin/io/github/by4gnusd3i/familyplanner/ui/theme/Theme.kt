package io.github.by4gnusd3i.familyplanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FamilyPlannerColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF0F766E),
    tertiary = Color(0xFFBE185D),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE0F2FE),
)

@Composable
fun FamilyPlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FamilyPlannerColors,
        content = content,
    )
}
