package io.github.fourgnusd3i.familyplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "household_profiles")
data class HouseholdProfileEntity(
    @PrimaryKey val id: Long = 1,
    val familyName: String,
    val createdAt: Instant = Instant.now(),
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String = "#3b82f6",
    val avatarUri: String? = null,
    val birthday: LocalDate? = null,
    val bio: String? = null,
    val createdAt: Instant = Instant.now(),
)

@Entity(
    tableName = "planner_events",
    indices = [
        Index("eventDate"),
        Index("ownerId"),
        Index("recurrenceType"),
        Index("sourceType"),
        Index("sourceMemberId"),
        Index("sourceYear"),
    ],
)
data class PlannerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val eventDate: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val recurrenceType: String? = null,
    val recurrenceUntil: LocalDate? = null,
    val ownerId: Long? = null,
    val color: String? = null,
    val note: String? = null,
    val sourceType: String? = null,
    val sourceMemberId: Long? = null,
    val sourceYear: Int? = null,
    val seriesStartDate: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
)

@Entity(tableName = "meal_plans", indices = [Index("dayOfWeek"), Index("ownerId")])
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val mealType: String,
    val meal: String,
    val ownerId: Long? = null,
    val note: String? = null,
    val createdAt: Instant = Instant.now(),
)

@Entity(tableName = "budget_months", indices = [Index(value = ["month"], unique = true)])
data class BudgetMonthEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,
    val limit: BigDecimal,
    val income: BigDecimal,
    val currencyCode: String,
    val updatedAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
)

@Entity(tableName = "expenses", indices = [Index("month"), Index("ownerId")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: BigDecimal,
    val category: String,
    val expenseDate: LocalDate,
    val ownerId: Long? = null,
    val description: String? = null,
    val month: String,
    val createdAt: Instant = Instant.now(),
)

@Entity(tableName = "notes", indices = [Index("ownerId"), Index("createdAt")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val ownerId: Long? = null,
    val content: String? = null,
    val createdAt: Instant = Instant.now(),
)

@Entity(tableName = "shopping_items", indices = [Index("ownerId"), Index("done"), Index("doneAt")])
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val item: String,
    val ownerId: Long? = null,
    val quantity: Int = 1,
    val done: Boolean = false,
    val doneAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
)
