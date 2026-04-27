package io.github.fourgnusd3i.familyplanner.data.repository

import io.github.fourgnusd3i.familyplanner.domain.model.PlannerDashboard
import kotlinx.coroutines.flow.Flow

interface PlannerRepository {
    fun observeDashboard(): Flow<PlannerDashboard>
}
