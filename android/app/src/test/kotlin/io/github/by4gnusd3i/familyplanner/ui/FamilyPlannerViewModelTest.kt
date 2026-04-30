package io.github.by4gnusd3i.familyplanner.ui

import io.github.by4gnusd3i.familyplanner.core.time.DateTimeProvider
import io.github.by4gnusd3i.familyplanner.data.repository.PlannerRepository
import io.github.by4gnusd3i.familyplanner.data.settings.AppSettings
import io.github.by4gnusd3i.familyplanner.domain.model.BudgetSnapshot
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerDashboard
import io.github.by4gnusd3i.familyplanner.domain.planner.BudgetMonthInput
import io.github.by4gnusd3i.familyplanner.domain.planner.EventInput
import io.github.by4gnusd3i.familyplanner.domain.planner.ExpenseInput
import io.github.by4gnusd3i.familyplanner.domain.planner.FamilyMemberInput
import io.github.by4gnusd3i.familyplanner.domain.planner.MealPlanInput
import io.github.by4gnusd3i.familyplanner.domain.planner.NoteInput
import io.github.by4gnusd3i.familyplanner.domain.planner.ShoppingItemInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class FamilyPlannerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedDate = LocalDate.of(2026, 4, 22)
    private val fixedNow = LocalDateTime.of(2026, 4, 22, 10, 15)
    private val dateTimeProvider = FakeDateTimeProvider(fixedDate, fixedNow)

    @Test
    fun initializesShoppingCleanup() = runTest {
        val repository = FakePlannerRepository()

        FamilyPlannerViewModel(repository, dateTimeProvider)

        assertEquals(1, repository.cleanupCalls)
    }

    @Test
    fun quickActionsSendExpectedInputsToRepository() = runTest {
        val repository = FakePlannerRepository()
        val viewModel = FamilyPlannerViewModel(repository, dateTimeProvider)

        viewModel.addEvent("Tannlege", "Husk kort")
        viewModel.addMeal("Taco", "Mais")
        viewModel.addExpense("149,90", "Mat")
        viewModel.addNote("Pakkliste", "Sko")
        viewModel.addShoppingItem("Melk", "")
        viewModel.saveBudgetMonth("26500", "82000,50", "nok", "2026-04")

        assertEquals(EventInput(title = "Tannlege", eventDate = fixedDate, note = "Husk kort"), repository.events.single())
        assertEquals(MealPlanInput(dayOfWeek = 2, meal = "Taco", note = "Mais"), repository.meals.single())
        assertEquals(
            ExpenseInput(amount = BigDecimal("149.90"), category = "Mat", expenseDate = fixedDate),
            repository.expenses.single(),
        )
        assertEquals(NoteInput(title = "Pakkliste", content = "Sko"), repository.notes.single())
        assertEquals(ShoppingItemInput(item = "Melk", quantity = 1), repository.shoppingItems.single())
        assertEquals(
            BudgetMonthInput(
                month = YearMonth.of(2026, 4),
                limit = BigDecimal("26500"),
                income = BigDecimal("82000.50"),
                currencyCode = "nok",
            ),
            repository.budgets.single(),
        )
        assertNull(viewModel.actionError.value)
    }

    @Test
    fun editAndDeleteActionsSendExpectedRepositoryCalls() = runTest {
        val repository = FakePlannerRepository()
        val viewModel = FamilyPlannerViewModel(repository, dateTimeProvider)
        val editedDate = fixedDate.plusDays(1)

        viewModel.saveEvent(7, "Oppdatert", "Detalj", editedDate)
        viewModel.saveMeal(8, 4, "Pizza", "Salat")
        viewModel.saveExpense(9, "250", "Transport", editedDate)
        viewModel.saveNote(10, "Notat", "Tekst")
        viewModel.saveShoppingItem(11, "Brød", "2")
        viewModel.deleteEvent(7)
        viewModel.deleteMeal(8)
        viewModel.deleteExpense(9)
        viewModel.deleteNote(10)
        viewModel.deleteShoppingItem(11)
        viewModel.toggleShoppingItem(11)

        assertEquals(EventInput(id = 7, title = "Oppdatert", eventDate = editedDate, note = "Detalj"), repository.events.single())
        assertEquals(MealPlanInput(id = 8, dayOfWeek = 4, meal = "Pizza", note = "Salat"), repository.meals.single())
        assertEquals(
            ExpenseInput(id = 9, amount = BigDecimal("250"), category = "Transport", expenseDate = editedDate),
            repository.expenses.single(),
        )
        assertEquals(NoteInput(id = 10, title = "Notat", content = "Tekst"), repository.notes.single())
        assertEquals(ShoppingItemInput(id = 11, item = "Brød", quantity = 2), repository.shoppingItems.single())
        assertEquals(listOf(7L), repository.deletedEvents)
        assertEquals(listOf(8L), repository.deletedMeals)
        assertEquals(listOf(9L), repository.deletedExpenses)
        assertEquals(listOf(10L), repository.deletedNotes)
        assertEquals(listOf(11L), repository.deletedShoppingItems)
        assertEquals(listOf(11L), repository.toggledShoppingItems)
    }

    @Test
    fun familyAndSettingsActionsSendExpectedRepositoryCalls() = runTest {
        val repository = FakePlannerRepository()
        val viewModel = FamilyPlannerViewModel(repository, dateTimeProvider)

        viewModel.saveFamilyMember(12, "Liv", "#10b981", "2018-05-14", "Liker fotball", "content://avatar")
        viewModel.deleteFamilyMember(13)
        viewModel.setLanguageOverride("nb")
        viewModel.setLanguageOverride(null)
        viewModel.setCurrencyCode("nok")

        assertEquals(
            FamilyMemberInput(
                id = 12,
                name = "Liv",
                color = "#10b981",
                avatarUri = "content://avatar",
                birthday = LocalDate.of(2018, 5, 14),
                bio = "Liker fotball",
            ),
            repository.familyMembers.single(),
        )
        assertEquals(listOf(13L), repository.deletedFamilyMembers)
        assertEquals(listOf("nb", null), repository.languageOverrides)
        assertEquals(listOf("nok"), repository.currencyCodes)
    }

    @Test
    fun repositoryFailureSurfacesActionErrorAndCanBeCleared() = runTest {
        val repository = FakePlannerRepository()
        repository.failNextAction = IllegalArgumentException("invalid event")
        val viewModel = FamilyPlannerViewModel(repository, dateTimeProvider)

        viewModel.addEvent("Tannlege", null)

        assertEquals("invalid event", viewModel.actionError.value)

        viewModel.clearActionError()

        assertNull(viewModel.actionError.value)
    }

    @Test
    fun invalidFamilyBirthdaySurfacesActionError() = runTest {
        val repository = FakePlannerRepository()
        val viewModel = FamilyPlannerViewModel(repository, dateTimeProvider)

        viewModel.saveFamilyMember(12, "Liv", "#10b981", "14.05.2018", null, null)

        assertTrue(viewModel.actionError.value?.contains("could not be parsed") == true)
        assertTrue(repository.familyMembers.isEmpty())
    }

    @Test
    fun invalidBudgetMonthSurfacesActionError() = runTest {
        val repository = FakePlannerRepository()
        val viewModel = FamilyPlannerViewModel(repository, dateTimeProvider)

        viewModel.saveBudgetMonth("1000", "2000", "NOK", "04-2026")

        assertTrue(viewModel.actionError.value?.contains("could not be parsed") == true)
        assertTrue(repository.budgets.isEmpty())
    }

    @Test
    fun setupFailureSurfacesSetupError() = runTest {
        val repository = FakePlannerRepository()
        repository.failSetup = IllegalArgumentException("missing setup")
        val viewModel = FamilyPlannerViewModel(repository, dateTimeProvider)

        viewModel.initializeHousehold("", "")

        assertEquals("missing setup", viewModel.setupError.value)
    }

    @Test
    fun successfulSetupClearsPreviousSetupError() = runTest {
        val repository = FakePlannerRepository()
        repository.failSetup = IllegalArgumentException("missing setup")
        val viewModel = FamilyPlannerViewModel(repository, dateTimeProvider)

        viewModel.initializeHousehold("", "")
        assertEquals("missing setup", viewModel.setupError.value)

        repository.failSetup = null
        viewModel.initializeHousehold("Hansen", "Anna")

        assertNull(viewModel.setupError.value)
        assertEquals("Hansen" to "Anna", repository.setupRequests.single())
    }

    private class FakeDateTimeProvider(
        private val today: LocalDate,
        private val now: LocalDateTime,
    ) : DateTimeProvider {
        override fun today(): LocalDate = today
        override fun now(): LocalDateTime = now
        override fun instantNow(): Instant = Instant.parse("2026-04-22T08:15:00Z")
    }

    private class FakePlannerRepository : PlannerRepository {
        private val dashboard = MutableStateFlow(emptyDashboard())
        private val settings = MutableStateFlow(AppSettings(languageOverride = null, currencyCode = "USD"))
        val setupRequests = mutableListOf<Pair<String, String>>()
        val familyMembers = mutableListOf<FamilyMemberInput>()
        val events = mutableListOf<EventInput>()
        val meals = mutableListOf<MealPlanInput>()
        val budgets = mutableListOf<BudgetMonthInput>()
        val expenses = mutableListOf<ExpenseInput>()
        val notes = mutableListOf<NoteInput>()
        val shoppingItems = mutableListOf<ShoppingItemInput>()
        val deletedEvents = mutableListOf<Long>()
        val deletedMeals = mutableListOf<Long>()
        val deletedExpenses = mutableListOf<Long>()
        val deletedNotes = mutableListOf<Long>()
        val deletedShoppingItems = mutableListOf<Long>()
        val deletedFamilyMembers = mutableListOf<Long>()
        val toggledShoppingItems = mutableListOf<Long>()
        val languageOverrides = mutableListOf<String?>()
        val currencyCodes = mutableListOf<String>()
        var cleanupCalls = 0
        var failSetup: RuntimeException? = null
        var failNextAction: RuntimeException? = null

        override fun observeDashboard(): Flow<PlannerDashboard> = dashboard

        override fun observeSettings(): Flow<AppSettings> = settings

        override suspend fun setLanguageOverride(languageId: String?) {
            languageOverrides += languageId
        }

        override suspend fun setCurrencyCode(currencyCode: String) {
            currencyCodes += currencyCode
        }

        override suspend fun initializeHousehold(familyName: String, firstMemberName: String) {
            failSetup?.let { throw it }
            setupRequests += familyName to firstMemberName
        }

        override suspend fun upsertFamilyMember(input: FamilyMemberInput): Long {
            familyMembers += input
            return input.id ?: familyMembers.size.toLong()
        }

        override suspend fun deleteFamilyMember(id: Long) {
            deletedFamilyMembers += id
        }

        override suspend fun upsertEvent(input: EventInput): Long {
            failNextAction?.let {
                failNextAction = null
                throw it
            }
            events += input
            return events.size.toLong()
        }

        override suspend fun deleteEvent(id: Long) {
            deletedEvents += id
        }

        override suspend fun upsertMeal(input: MealPlanInput): Long {
            meals += input
            return meals.size.toLong()
        }

        override suspend fun deleteMeal(id: Long) {
            deletedMeals += id
        }

        override suspend fun upsertBudgetMonth(input: BudgetMonthInput): Long {
            budgets += input
            return budgets.size.toLong()
        }

        override suspend fun upsertExpense(input: ExpenseInput): Long {
            failNextAction?.let {
                failNextAction = null
                throw it
            }
            expenses += input
            return expenses.size.toLong()
        }

        override suspend fun deleteExpense(id: Long) {
            deletedExpenses += id
        }

        override suspend fun upsertNote(input: NoteInput): Long {
            notes += input
            return notes.size.toLong()
        }

        override suspend fun deleteNote(id: Long) {
            deletedNotes += id
        }

        override suspend fun upsertShoppingItem(input: ShoppingItemInput): Long {
            shoppingItems += input
            return shoppingItems.size.toLong()
        }

        override suspend fun toggleShoppingItem(id: Long) {
            toggledShoppingItems += id
        }

        override suspend fun deleteShoppingItem(id: Long) {
            deletedShoppingItems += id
        }

        override suspend fun cleanupDoneShoppingItems() {
            cleanupCalls += 1
        }

        private fun unsupported(): Nothing =
            error("Not used by this test")
    }

    private companion object {
        fun emptyDashboard(): PlannerDashboard =
            PlannerDashboard(
                isSetupComplete = false,
                familyMembers = emptyList(),
                weekStart = LocalDate.of(2026, 4, 20),
                weekEvents = emptyList(),
                upcomingEvents = emptyList(),
                meals = emptyList(),
                budget = BudgetSnapshot(
                    month = "",
                    limit = BigDecimal.ZERO,
                    income = BigDecimal.ZERO,
                    spent = BigDecimal.ZERO,
                    remaining = BigDecimal.ZERO,
                    available = BigDecimal.ZERO,
                    currencyCode = "USD",
                    expenses = emptyList(),
                ),
                shoppingItems = emptyList(),
                notes = emptyList(),
            )
    }
}
