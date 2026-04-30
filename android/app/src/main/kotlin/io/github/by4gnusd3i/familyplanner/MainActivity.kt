package io.github.by4gnusd3i.familyplanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.by4gnusd3i.familyplanner.ui.FamilyPlannerApp
import io.github.by4gnusd3i.familyplanner.ui.FamilyPlannerViewModel
import io.github.by4gnusd3i.familyplanner.ui.theme.FamilyPlannerTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
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
