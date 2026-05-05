package io.github.by4gnusd3i.familyplanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.by4gnusd3i.familyplanner.ui.FamilyPlannerInteractiveParityTestContent
import io.github.by4gnusd3i.familyplanner.ui.FamilyPlannerTabletDashboardTestContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabletDashboardUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabletDashboardMatchesReleaseInformationHierarchy() {
        composeRule.setContent {
            FamilyPlannerTabletDashboardTestContent()
        }

        listOf(
            "+ Appointment",
            "+ Meal",
            "+ Expense",
            "+ Note",
            "+ Item",
            "Anna",
            "Add member",
            "Budget",
            "Shopping list",
            "Notes",
            "Weekly plan",
            "Meals",
            "Upcoming",
        ).forEach { label ->
            composeRule.onAllNodesWithText(label)[0].assertIsDisplayed()
        }
    }

    @Test
    fun tabletDashboardShowsAllSevenWeekColumns() {
        composeRule.setContent {
            FamilyPlannerTabletDashboardTestContent()
        }

        listOf("20.04.", "21.04.", "22.04.", "23.04.", "24.04.", "25.04.", "26.04.")
            .forEach { day ->
                composeRule.onAllNodesWithText(day)[0].assertIsDisplayed()
            }
    }

    @Test
    fun tabletDashboardShowsWeekNavigationControls() {
        composeRule.setContent {
            FamilyPlannerTabletDashboardTestContent()
        }

        listOf("Previous", "This week", "Next").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun quickActionDialogsExposeReleaseParityFields() {
        composeRule.setContent {
            FamilyPlannerInteractiveParityTestContent()
        }

        composeRule.onNodeWithText("+ Appointment").performClick()
        listOf("Date (YYYY-MM-DD)", "Start (HH:MM)", "End (HH:MM)", "Recurrence", "Responsible").forEach { label ->
            composeRule.onAllNodesWithText(label)[0].assertIsDisplayed()
        }
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("+ Meal").performClick()
        listOf("Meal", "Day", "Meal type", "Responsible").forEach { label ->
            composeRule.onAllNodesWithText(label)[0].assertIsDisplayed()
        }
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("+ Expense").performClick()
        listOf("Amount", "Category", "Expense date (YYYY-MM-DD)", "Responsible", "Description").forEach { label ->
            composeRule.onAllNodesWithText(label)[0].assertIsDisplayed()
        }
    }

    @Test
    fun tabletDashboardKeepsFamilyRowAsFooter() {
        composeRule.setContent {
            FamilyPlannerTabletDashboardTestContent()
        }

        val familyTile = composeRule.onNodeWithText("Anna").getUnclippedBoundsInRoot()
        val weekPanel = composeRule.onAllNodesWithText("Weekly plan")[0].getUnclippedBoundsInRoot()
        val mealPanel = composeRule.onNodeWithText("Meals").getUnclippedBoundsInRoot()

        assertTrue("Family row should sit below the main dashboard grid.", familyTile.top > weekPanel.top)
        assertTrue("Family row should sit below the meals panel header.", familyTile.top > mealPanel.top)
    }
}
