package io.github.fourgnusd3i.familyplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.fourgnusd3i.familyplanner.ui.FamilyPlannerApp
import io.github.fourgnusd3i.familyplanner.ui.theme.FamilyPlannerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyPlannerTheme {
                FamilyPlannerApp()
            }
        }
    }
}
