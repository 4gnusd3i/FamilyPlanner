package io.github.by4gnusd3i.familyplanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.by4gnusd3i.familyplanner.ui.FamilyPlannerPhoneShellTestContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneShellUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun phoneShellShowsNavigationAndQuickActionChooser() {
        composeRule.setContent {
            FamilyPlannerPhoneShellTestContent()
        }

        listOf("Overview", "Week", "Meals", "Lists", "Budget").forEach { label ->
            composeRule.onAllNodesWithText(label)[0].assertIsDisplayed()
        }

        composeRule.onNodeWithContentDescription("Add to planner").performClick()
        composeRule.onNodeWithText("Add to planner").assertIsDisplayed()
        listOf("Appointment", "Meal", "Expense", "Note", "Item").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }
}
