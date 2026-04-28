package io.github.by4gnusd3i.familyplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        HouseholdProfileEntity::class,
        FamilyMemberEntity::class,
        PlannerEventEntity::class,
        MealPlanEntity::class,
        BudgetMonthEntity::class,
        ExpenseEntity::class,
        NoteEntity::class,
        ShoppingItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FamilyPlannerDatabase : RoomDatabase() {
    abstract fun plannerDao(): PlannerDao
}
