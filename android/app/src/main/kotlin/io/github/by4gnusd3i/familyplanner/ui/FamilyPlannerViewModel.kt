package io.github.by4gnusd3i.familyplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.by4gnusd3i.familyplanner.core.time.DateTimeProvider
import io.github.by4gnusd3i.familyplanner.data.repository.PlannerRepository
import io.github.by4gnusd3i.familyplanner.domain.model.BudgetSnapshot
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerDashboard
import io.github.by4gnusd3i.familyplanner.domain.planner.EventInput
import io.github.by4gnusd3i.familyplanner.domain.planner.ExpenseInput
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
        runPlannerAction {
            repository.upsertEvent(
                EventInput(
                    title = title,
                    eventDate = dateTimeProvider.today(),
                    note = note,
                ),
            )
        }
    }

    fun addMeal(meal: String, note: String?) {
        runPlannerAction {
            repository.upsertMeal(
                MealPlanInput(
                    dayOfWeek = dateTimeProvider.today().dayOfWeek.toPlannerDayIndex(),
                    meal = meal,
                    note = note,
                ),
            )
        }
    }

    fun addExpense(amount: String, category: String?) {
        runPlannerAction {
            repository.upsertExpense(
                ExpenseInput(
                    amount = amount.trim().replace(',', '.').toBigDecimal(),
                    category = category,
                    expenseDate = dateTimeProvider.today(),
                ),
            )
        }
    }

    fun addNote(title: String, content: String?) {
        runPlannerAction {
            repository.upsertNote(
                NoteInput(
                    title = title,
                    content = content,
                ),
            )
        }
    }

    fun addShoppingItem(item: String, quantity: String) {
        runPlannerAction {
            repository.upsertShoppingItem(
                ShoppingItemInput(
                    item = item,
                    quantity = quantity.trim().toIntOrNull() ?: 1,
                ),
            )
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
