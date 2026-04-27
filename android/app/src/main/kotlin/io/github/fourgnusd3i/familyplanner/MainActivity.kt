package io.github.fourgnusd3i.familyplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.fourgnusd3i.familyplanner.ui.FamilyPlannerApp
import io.github.fourgnusd3i.familyplanner.ui.FamilyPlannerViewModel
import io.github.fourgnusd3i.familyplanner.ui.theme.FamilyPlannerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: FamilyPlannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyPlannerTheme {
                FamilyPlannerApp(viewModel)
            }
        }
    }
}
