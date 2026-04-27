package io.github.fourgnusd3i.familyplanner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface PlannerDao {
    @Query("SELECT COUNT(*) FROM household_profiles WHERE id = 1")
    fun observeHouseholdCount(): Flow<Int>

    @Query("SELECT * FROM household_profiles WHERE id = 1")
    fun observeHouseholdProfile(): Flow<HouseholdProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHouseholdProfile(profile: HouseholdProfileEntity)

    @Transaction
    suspend fun initializeHousehold(profile: HouseholdProfileEntity, firstMember: FamilyMemberEntity) {
        upsertHouseholdProfile(profile)
        upsertFamilyMember(firstMember)
    }

    @Query("SELECT * FROM family_members ORDER BY createdAt")
    fun observeFamilyMembers(): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFamilyMember(member: FamilyMemberEntity): Long

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteFamilyMember(id: Long)

    @Query("SELECT * FROM planner_events WHERE eventDate BETWEEN :start AND :end ORDER BY eventDate, startTime, title")
    fun observeEventsInRange(start: LocalDate, end: LocalDate): Flow<List<PlannerEventEntity>>

    @Query("SELECT * FROM planner_events ORDER BY eventDate, startTime, title")
    fun observeAllEvents(): Flow<List<PlannerEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: PlannerEventEntity): Long

    @Delete
    suspend fun deleteEvent(event: PlannerEventEntity)

    @Query("SELECT * FROM meal_plans ORDER BY dayOfWeek, mealType, createdAt")
    fun observeMeals(): Flow<List<MealPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeal(meal: MealPlanEntity): Long

    @Query("DELETE FROM meal_plans WHERE id = :id")
    suspend fun deleteMeal(id: Long)

    @Query("SELECT * FROM budget_months WHERE month = :month LIMIT 1")
    fun observeBudgetMonth(month: String): Flow<BudgetMonthEntity?>

    @Query("SELECT * FROM expenses WHERE month = :month ORDER BY expenseDate DESC, createdAt DESC")
    fun observeExpenses(month: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgetMonth(month: BudgetMonthEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT * FROM shopping_items ORDER BY createdAt DESC")
    fun observeShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoppingItem(item: ShoppingItemEntity): Long

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteShoppingItem(id: Long)

    @Query("DELETE FROM shopping_items WHERE done = 1 AND doneAt IS NOT NULL AND doneAt <= :cutoff")
    suspend fun deleteExpiredDoneShoppingItems(cutoff: Instant)

    @Transaction
    suspend fun resetAll() {
        clearHouseholdProfiles()
        clearFamilyMembers()
        clearEvents()
        clearMeals()
        clearBudgetMonths()
        clearExpenses()
        clearNotes()
        clearShoppingItems()
    }

    @Query("DELETE FROM household_profiles")
    suspend fun clearHouseholdProfiles()

    @Query("DELETE FROM family_members")
    suspend fun clearFamilyMembers()

    @Query("DELETE FROM planner_events")
    suspend fun clearEvents()

    @Query("DELETE FROM meal_plans")
    suspend fun clearMeals()

    @Query("DELETE FROM budget_months")
    suspend fun clearBudgetMonths()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("DELETE FROM shopping_items")
    suspend fun clearShoppingItems()
}
