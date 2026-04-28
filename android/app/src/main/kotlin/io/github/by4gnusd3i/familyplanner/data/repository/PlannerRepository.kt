package io.github.by4gnusd3i.familyplanner.data.repository

import io.github.by4gnusd3i.familyplanner.data.settings.AppSettings
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerDashboard
import io.github.by4gnusd3i.familyplanner.domain.planner.BudgetMonthInput
import io.github.by4gnusd3i.familyplanner.domain.planner.EventInput
import io.github.by4gnusd3i.familyplanner.domain.planner.ExpenseInput
import io.github.by4gnusd3i.familyplanner.domain.planner.FamilyMemberInput
import io.github.by4gnusd3i.familyplanner.domain.planner.MealPlanInput
import io.github.by4gnusd3i.familyplanner.domain.planner.NoteInput
import io.github.by4gnusd3i.familyplanner.domain.planner.ShoppingItemInput
import kotlinx.coroutines.flow.Flow

interface PlannerRepository {
    fun observeDashboard(): Flow<PlannerDashboard>
    fun observeSettings(): Flow<AppSettings>
    suspend fun setLanguageOverride(languageId: String?)
    suspend fun setCurrencyCode(currencyCode: String)
    suspend fun initializeHousehold(familyName: String, firstMemberName: String)
    suspend fun upsertFamilyMember(input: FamilyMemberInput): Long
    suspend fun deleteFamilyMember(id: Long)
    suspend fun upsertEvent(input: EventInput): Long
    suspend fun deleteEvent(id: Long)
    suspend fun upsertMeal(input: MealPlanInput): Long
    suspend fun deleteMeal(id: Long)
    suspend fun upsertBudgetMonth(input: BudgetMonthInput): Long
    suspend fun upsertExpense(input: ExpenseInput): Long
    suspend fun deleteExpense(id: Long)
    suspend fun upsertNote(input: NoteInput): Long
    suspend fun deleteNote(id: Long)
    suspend fun upsertShoppingItem(input: ShoppingItemInput): Long
    suspend fun toggleShoppingItem(id: Long)
    suspend fun deleteShoppingItem(id: Long)
    suspend fun cleanupDoneShoppingItems()
}
