package io.github.by4gnusd3i.familyplanner

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupScreenTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun freshInstallShowsSetupScreen() {
        resetAppData()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("Set up FamilyPlanner").assertIsDisplayed()
            composeRule.onNodeWithText("Birthday (YYYY-MM-DD)").assertIsDisplayed()
            composeRule.onNodeWithText("Bio").assertIsDisplayed()
            composeRule.onNodeWithText("Choose avatar").assertIsDisplayed()
        }
    }

    private fun resetAppData() {
        ApplicationProvider
            .getApplicationContext<Context>()
            .deleteDatabase("familyplanner.db")
    }
}
