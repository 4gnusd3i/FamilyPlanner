package io.github.by4gnusd3i.familyplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class FamilyPlannerViewModel @Inject constructor(
    private val repository: PlannerRepository,
    private val dateTimeProvider: DateTimeProvider,
) : ViewModel() {
    val dashboard: StateFlow<PlannerDashboard> =
        repository.observeDashboard()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PlannerDashboard(
                    isSetupComplete = false,
                    familyMembers = emptyList(),
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
                ),
            )

    val settings: StateFlow<AppSettings> =
        repository.observeSettings()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AppSettings(languageOverride = null, currencyCode = "USD"),
            )

    private val _setupError = MutableStateFlow<String?>(null)
    val setupError: StateFlow<String?> = _setupError.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.cleanupDoneShoppingItems()
        }
    }

    fun initializeHousehold(familyName: String, firstMemberName: String) {
        viewModelScope.launch {
            runCatching {
                repository.initializeHousehold(familyName, firstMemberName)
            }.onSuccess {
                _setupError.value = null
            }.onFailure { error ->
                _setupError.value = error.message
            }
        }
    }

    fun addEvent(title: String, note: String?) {
        saveEvent(
            id = null,
            title = title,
            note = note,
            eventDate = dateTimeProvider.today(),
        )
    }

    fun saveEvent(id: Long?, title: String, note: String?, eventDate: LocalDate) {
        runPlannerAction {
            repository.upsertEvent(
                EventInput(
                    id = id,
                    title = title,
                    eventDate = eventDate,
                    note = note,
                ),
            )
        }
    }

    fun addMeal(meal: String, note: String?) {
        saveMeal(
            id = null,
            dayOfWeek = dateTimeProvider.today().dayOfWeek.toPlannerDayIndex(),
            meal = meal,
            note = note,
        )
    }

    fun saveMeal(id: Long?, dayOfWeek: Int, meal: String, note: String?) {
        runPlannerAction {
            repository.upsertMeal(
                MealPlanInput(
                    id = id,
                    dayOfWeek = dayOfWeek,
                    meal = meal,
                    note = note,
                ),
            )
        }
    }

    fun addExpense(amount: String, category: String?) {
        saveExpense(
            id = null,
            amount = amount,
            category = category,
            expenseDate = dateTimeProvider.today(),
        )
    }

    fun saveExpense(id: Long?, amount: String, category: String?, expenseDate: LocalDate) {
        runPlannerAction {
            repository.upsertExpense(
                ExpenseInput(
                    id = id,
                    amount = amount.trim().replace(',', '.').toBigDecimal(),
                    category = category,
                    expenseDate = expenseDate,
                ),
            )
        }
    }

    fun addNote(title: String, content: String?) {
        saveNote(id = null, title = title, content = content)
    }

    fun saveNote(id: Long?, title: String, content: String?) {
        runPlannerAction {
            repository.upsertNote(
                NoteInput(
                    id = id,
                    title = title,
                    content = content,
                ),
            )
        }
    }

    fun addShoppingItem(item: String, quantity: String) {
        saveShoppingItem(id = null, item = item, quantity = quantity)
    }

    fun saveShoppingItem(id: Long?, item: String, quantity: String) {
        runPlannerAction {
            repository.upsertShoppingItem(
                ShoppingItemInput(
                    id = id,
                    item = item,
                    quantity = quantity.trim().toIntOrNull() ?: 1,
                ),
            )
        }
    }

    fun saveBudgetMonth(limit: String, income: String, currencyCode: String, month: String) {
        runPlannerAction {
            repository.upsertBudgetMonth(
                BudgetMonthInput(
                    month = YearMonth.parse(month.trim()),
                    limit = limit.trim().replace(',', '.').toBigDecimal(),
                    income = income.trim().replace(',', '.').toBigDecimal(),
                    currencyCode = currencyCode,
                ),
            )
        }
    }

    fun deleteEvent(id: Long) {
        runPlannerAction {
            repository.deleteEvent(id)
        }
    }

    fun deleteMeal(id: Long) {
        runPlannerAction {
            repository.deleteMeal(id)
        }
    }

    fun deleteExpense(id: Long) {
        runPlannerAction {
            repository.deleteExpense(id)
        }
    }

    fun deleteNote(id: Long) {
        runPlannerAction {
            repository.deleteNote(id)
        }
    }

    fun deleteShoppingItem(id: Long) {
        runPlannerAction {
            repository.deleteShoppingItem(id)
        }
    }

    fun toggleShoppingItem(id: Long) {
        runPlannerAction {
            repository.toggleShoppingItem(id)
        }
    }

    fun saveFamilyMember(
        id: Long?,
        name: String,
        color: String?,
        birthday: String,
        bio: String?,
        avatarUri: String?,
    ) {
        runPlannerAction {
            repository.upsertFamilyMember(
                FamilyMemberInput(
                    id = id,
                    name = name,
                    color = color,
                    birthday = birthday.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                    bio = bio,
                    avatarUri = avatarUri,
                ),
            )
        }
    }

    fun deleteFamilyMember(id: Long) {
        runPlannerAction {
            repository.deleteFamilyMember(id)
        }
    }

    fun setLanguageOverride(languageId: String?) {
        runPlannerAction {
            repository.setLanguageOverride(languageId)
        }
    }

    fun setCurrencyCode(currencyCode: String) {
        runPlannerAction {
            repository.setCurrencyCode(currencyCode)
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    private fun runPlannerAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching {
                action()
            }.onSuccess {
                _actionError.value = null
            }.onFailure { error ->
                _actionError.value = error.message
            }
        }
    }

    private fun DayOfWeek.toPlannerDayIndex(): Int =
        value - 1
}
