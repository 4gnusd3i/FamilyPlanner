package io.github.by4gnusd3i.familyplanner.domain.planner

import io.github.by4gnusd3i.familyplanner.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class PlannerInputNormalizerTest {
    @Test
    fun familyMemberTrimsNameAndNormalizesColor() {
        val member = PlannerInputNormalizer.familyMember(
            FamilyMemberInput(name = "  Anna  ", color = "  #ABCDEF  ", bio = "  "),
        )

        assertEquals("Anna", member.name)
        assertEquals("#abcdef", member.color)
        assertEquals(null, member.bio)
    }

    @Test
    fun invalidFamilyColorFallsBackToDefault() {
        val member = PlannerInputNormalizer.familyMember(
            FamilyMemberInput(name = "Anna", color = "pink"),
        )

        assertEquals(PlannerInputNormalizer.DEFAULT_MEMBER_COLOR, member.color)
    }

    @Test
    fun recurringEventRequiresValidUntilDate() {
        assertThrows(IllegalArgumentException::class.java) {
            PlannerInputNormalizer.event(
                EventInput(
                    title = "Fotball",
                    eventDate = LocalDate.of(2026, 4, 20),
                    recurrenceType = RecurrenceType.Weekly,
                    recurrenceUntil = LocalDate.of(2026, 4, 19),
                ),
            )
        }
    }

    @Test
    fun recurrenceUntilRequiresRecurrenceType() {
        assertThrows(IllegalArgumentException::class.java) {
            PlannerInputNormalizer.event(
                EventInput(
                    title = "Fotball",
                    eventDate = LocalDate.of(2026, 4, 20),
                    recurrenceUntil = LocalDate.of(2026, 4, 21),
                ),
            )
        }
    }

    @Test
    fun mealRequiresCalendarDayInsideWeek() {
        assertThrows(IllegalArgumentException::class.java) {
            PlannerInputNormalizer.meal(
                MealPlanInput(dayOfWeek = 7, meal = "Taco"),
            )
        }
    }

    @Test
    fun shoppingQuantityMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) {
            PlannerInputNormalizer.shoppingItem(
                ShoppingItemInput(item = "Melk", quantity = 0),
            )
        }
    }

    @Test
    fun budgetNormalizesCurrencyCodeAndRejectsNegativeValues() {
        val budget = PlannerInputNormalizer.budgetMonth(
            BudgetMonthInput(
                month = YearMonth.of(2026, 4),
                limit = BigDecimal("1000"),
                income = BigDecimal("2500"),
                currencyCode = " nok ",
            ),
        )

        assertEquals("NOK", budget.currencyCode)

        assertThrows(IllegalArgumentException::class.java) {
            PlannerInputNormalizer.budgetMonth(
                BudgetMonthInput(
                    month = YearMonth.of(2026, 4),
                    limit = BigDecimal("-1"),
                    income = BigDecimal.ZERO,
                    currencyCode = "NOK",
                ),
            )
        }
    }

    @Test
    fun expenseUsesDefaultCategoryAndRequiresPositiveAmount() {
        val expense = PlannerInputNormalizer.expense(
            ExpenseInput(
                amount = BigDecimal("149.90"),
                expenseDate = LocalDate.of(2026, 4, 20),
                category = " ",
            ),
        )

        assertEquals(PlannerInputNormalizer.DEFAULT_EXPENSE_CATEGORY, expense.category)

        assertThrows(IllegalArgumentException::class.java) {
            PlannerInputNormalizer.expense(
                ExpenseInput(
                    amount = BigDecimal.ZERO,
                    expenseDate = LocalDate.of(2026, 4, 20),
                ),
            )
        }
    }
}
