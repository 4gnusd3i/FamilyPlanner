package io.github.fourgnusd3i.familyplanner.data.repository

import io.github.fourgnusd3i.familyplanner.core.time.DateTimeProvider
import io.github.fourgnusd3i.familyplanner.data.local.PlannerDao
import io.github.fourgnusd3i.familyplanner.data.local.toDomain
import io.github.fourgnusd3i.familyplanner.domain.model.PlannerDashboard
import io.github.fourgnusd3i.familyplanner.domain.planner.RecurrenceRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class RoomPlannerRepository @Inject constructor(
    private val dao: PlannerDao,
    private val dateTimeProvider: DateTimeProvider,
) : PlannerRepository {
    override fun observeDashboard(): Flow<PlannerDashboard> =
        combine(
            dao.observeHouseholdCount(),
            dao.observeFamilyMembers(),
            dao.observeAllEvents(),
            dao.observeShoppingItems(),
            dao.observeNotes(),
        ) { householdCount, family, events, shopping, notes ->
            val upcoming = RecurrenceRules.upcomingEvents(
                events = events.map { it.toDomain() },
                now = dateTimeProvider.now(),
            )
            PlannerDashboard(
                isSetupComplete = householdCount > 0,
                familyMembers = family.map { it.toDomain() },
                upcomingEvents = upcoming,
                shoppingItems = shopping.map { it.toDomain() },
                notes = notes.map { it.toDomain() },
            )
        }
}
