package io.github.fourgnusd3i.familyplanner.domain.planner

import io.github.fourgnusd3i.familyplanner.domain.model.RecurrenceType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.Locale

data class FamilyMemberInput(
    val id: Long? = null,
    val name: String,
    val color: String? = null,
    val avatarUri: String? = null,
    val birthday: LocalDate? = null,
    val bio: String? = null,
)

data class EventInput(
    val id: Long? = null,
    val title: String,
    val eventDate: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val recurrenceType: RecurrenceType? = null,
    val recurrenceUntil: LocalDate? = null,
    val ownerId: Long? = null,
    val note: String? = null,
)

data class MealPlanInput(
    val id: Long? = null,
    val dayOfWeek: Int,
    val mealType: String = "dinner",
    val meal: String,
    val ownerId: Long? = null,
    val note: String? = null,
)

data class BudgetMonthInput(
    val id: Long? = null,
    val month: YearMonth,
    val limit: BigDecimal,
    val income: BigDecimal,
    val currencyCode: String,
)

data class ExpenseInput(
    val id: Long? = null,
    val amount: BigDecimal,
    val category: String? = null,
    val expenseDate: LocalDate,
    val ownerId: Long? = null,
    val description: String? = null,
)

data class NoteInput(
    val id: Long? = null,
    val title: String,
    val ownerId: Long? = null,
    val content: String? = null,
)

data class ShoppingItemInput(
    val id: Long? = null,
    val item: String,
    val ownerId: Long? = null,
    val quantity: Int = 1,
)

object PlannerInputNormalizer {
    const val DEFAULT_MEMBER_COLOR = "#3b82f6"
    const val DEFAULT_EVENT_COLOR = "#eaf4ff"
    const val DEFAULT_MEAL_TYPE = "dinner"
    const val DEFAULT_EXPENSE_CATEGORY = "Annet"
    const val SHOPPING_DONE_RETENTION_SECONDS = 15L

    fun familyMember(input: FamilyMemberInput): FamilyMemberInput =
        input.copy(
            id = positiveIdOrNull(input.id),
            name = requiredText(input.name, "family member name"),
            color = normalizeColor(input.color),
            avatarUri = optionalText(input.avatarUri),
            bio = optionalText(input.bio),
        )

    fun event(input: EventInput): EventInput {
        if (input.recurrenceType == null && input.recurrenceUntil != null) {
            throw IllegalArgumentException("recurrence until requires recurrence")
        }

        if (input.recurrenceUntil != null && input.recurrenceUntil < input.eventDate) {
            throw IllegalArgumentException("recurrence until cannot be before event date")
        }

        return input.copy(
            id = positiveIdOrNull(input.id),
            title = requiredText(input.title, "event title"),
            ownerId = positiveIdOrNull(input.ownerId),
            note = optionalText(input.note),
        )
    }

    fun meal(input: MealPlanInput): MealPlanInput {
        require(input.dayOfWeek in 0..6) { "day of week must be 0 through 6" }

        return input.copy(
            id = positiveIdOrNull(input.id),
            mealType = optionalText(input.mealType) ?: DEFAULT_MEAL_TYPE,
            meal = requiredText(input.meal, "meal"),
            ownerId = positiveIdOrNull(input.ownerId),
            note = optionalText(input.note),
        )
    }

    fun budgetMonth(input: BudgetMonthInput): BudgetMonthInput {
        require(input.limit >= BigDecimal.ZERO) { "budget limit cannot be negative" }
        require(input.income >= BigDecimal.ZERO) { "budget income cannot be negative" }

        val currencyCode = requiredText(input.currencyCode, "currency code")
            .uppercase(Locale.ROOT)
        require(currencyCode.length == 3 && currencyCode.all { it in 'A'..'Z' }) {
            "currency code must be ISO 4217 style"
        }

        return input.copy(
            id = positiveIdOrNull(input.id),
            currencyCode = currencyCode,
        )
    }

    fun expense(input: ExpenseInput): ExpenseInput {
        require(input.amount > BigDecimal.ZERO) { "expense amount must be positive" }

        return input.copy(
            id = positiveIdOrNull(input.id),
            category = optionalText(input.category) ?: DEFAULT_EXPENSE_CATEGORY,
            ownerId = positiveIdOrNull(input.ownerId),
            description = optionalText(input.description),
        )
    }

    fun note(input: NoteInput): NoteInput =
        input.copy(
            id = positiveIdOrNull(input.id),
            title = requiredText(input.title, "note title"),
            ownerId = positiveIdOrNull(input.ownerId),
            content = optionalText(input.content),
        )

    fun shoppingItem(input: ShoppingItemInput): ShoppingItemInput {
        require(input.quantity > 0) { "shopping quantity must be positive" }

        return input.copy(
            id = positiveIdOrNull(input.id),
            item = requiredText(input.item, "shopping item"),
            ownerId = positiveIdOrNull(input.ownerId),
        )
    }

    private fun requiredText(value: String, fieldName: String): String =
        optionalText(value) ?: throw IllegalArgumentException("$fieldName is required")

    private fun optionalText(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }

    private fun positiveIdOrNull(value: Long?): Long? =
        value?.takeIf { it > 0 }

    private fun normalizeColor(value: String?): String {
        val color = optionalText(value) ?: return DEFAULT_MEMBER_COLOR
        val isHexColor = (color.length == 4 || color.length == 7) &&
            color.first() == '#' &&
            color.drop(1).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

        return if (isHexColor) color.lowercase(Locale.ROOT) else DEFAULT_MEMBER_COLOR
    }
}
