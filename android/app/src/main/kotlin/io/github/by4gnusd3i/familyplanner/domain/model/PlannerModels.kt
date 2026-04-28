package io.github.by4gnusd3i.familyplanner.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class RecurrenceType(val storageValue: String) {
    Daily("daily"),
    Weekly("weekly");

    companion object {
        fun fromStorage(value: String?): RecurrenceType? =
            entries.firstOrNull { it.storageValue == value }
    }
}

data class FamilyMember(
    val id: Long,
    val name: String,
    val color: String,
    val avatarUri: String?,
    val birthday: LocalDate?,
    val bio: String?,
)

data class PlannerEvent(
    val id: Long,
    val title: String,
    val eventDate: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val recurrenceType: RecurrenceType? = null,
    val recurrenceUntil: LocalDate? = null,
    val ownerId: Long? = null,
    val color: String? = null,
    val note: String? = null,
    val sourceType: String? = null,
    val sourceMemberId: Long? = null,
    val sourceYear: Int? = null,
    val seriesStartDate: LocalDate? = null,
)

data class MealPlan(
    val id: Long,
    val dayOfWeek: Int,
    val mealType: String,
    val meal: String,
    val ownerId: Long?,
    val note: String?,
)

data class BudgetSnapshot(
    val month: String,
    val limit: BigDecimal,
    val income: BigDecimal,
    val spent: BigDecimal,
    val remaining: BigDecimal,
    val available: BigDecimal,
    val currencyCode: String,
)

data class NoteItem(
    val id: Long,
    val title: String,
    val ownerId: Long?,
    val content: String?,
)

data class ShoppingItem(
    val id: Long,
    val item: String,
    val ownerId: Long?,
    val quantity: Int,
    val done: Boolean,
    val doneAt: Instant?,
)

data class PlannerDashboard(
    val isSetupComplete: Boolean,
    val familyMembers: List<FamilyMember>,
    val upcomingEvents: List<PlannerEvent>,
    val shoppingItems: List<ShoppingItem>,
    val notes: List<NoteItem>,
)
