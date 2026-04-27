package io.github.fourgnusd3i.familyplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.fourgnusd3i.familyplanner.data.repository.PlannerRepository
import io.github.fourgnusd3i.familyplanner.domain.model.PlannerDashboard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FamilyPlannerViewModel @Inject constructor(
    repository: PlannerRepository,
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
}
