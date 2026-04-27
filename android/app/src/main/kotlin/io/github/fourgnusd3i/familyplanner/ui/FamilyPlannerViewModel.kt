package io.github.fourgnusd3i.familyplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.fourgnusd3i.familyplanner.data.repository.PlannerRepository
import io.github.fourgnusd3i.familyplanner.domain.model.PlannerDashboard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyPlannerViewModel @Inject constructor(
    private val repository: PlannerRepository,
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
                    shoppingItems = emptyList(),
                    notes = emptyList(),
                ),
            )

    private val _setupError = MutableStateFlow<String?>(null)
    val setupError: StateFlow<String?> = _setupError.asStateFlow()

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
}
