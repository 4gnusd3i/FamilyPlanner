package io.github.by4gnusd3i.familyplanner

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
        ApplicationProvider
            .getApplicationContext<android.content.Context>()
            .deleteDatabase("familyplanner.db")

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("Set up FamilyPlanner").assertIsDisplayed()
        }
    }
}
