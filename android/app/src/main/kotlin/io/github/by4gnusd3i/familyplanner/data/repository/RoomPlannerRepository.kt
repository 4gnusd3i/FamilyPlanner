package io.github.by4gnusd3i.familyplanner.data.repository

import io.github.by4gnusd3i.familyplanner.core.time.DateTimeProvider
import io.github.by4gnusd3i.familyplanner.data.local.BudgetMonthEntity
import io.github.by4gnusd3i.familyplanner.data.local.ExpenseEntity
import io.github.by4gnusd3i.familyplanner.data.local.FamilyMemberEntity
import io.github.by4gnusd3i.familyplanner.data.local.HouseholdProfileEntity
import io.github.by4gnusd3i.familyplanner.data.local.MealPlanEntity
import io.github.by4gnusd3i.familyplanner.data.local.NoteEntity
import io.github.by4gnusd3i.familyplanner.data.local.PlannerDao
import io.github.by4gnusd3i.familyplanner.data.local.PlannerEventEntity
import io.github.by4gnusd3i.familyplanner.data.local.ShoppingItemEntity
import io.github.by4gnusd3i.familyplanner.data.local.toDomain
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerDashboard
import io.github.by4gnusd3i.familyplanner.domain.planner.BudgetMonthInput
import io.github.by4gnusd3i.familyplanner.domain.planner.EventInput
import io.github.by4gnusd3i.familyplanner.domain.planner.ExpenseInput
import io.github.by4gnusd3i.familyplanner.domain.planner.FamilyMemberInput
import io.github.by4gnusd3i.familyplanner.domain.planner.MealPlanInput
import io.github.by4gnusd3i.familyplanner.domain.planner.NoteInput
import io.github.by4gnusd3i.familyplanner.domain.planner.PlannerInputNormalizer
import io.github.by4gnusd3i.familyplanner.domain.planner.RecurrenceRules
import io.github.by4gnusd3i.familyplanner.domain.planner.ShoppingItemInput
import io.github.by4gnusd3i.familyplanner.domain.setup.SetupValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Duration
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

    override suspend fun initializeHousehold(familyName: String, firstMemberName: String) {
        val request = SetupValidator.validate(familyName, firstMemberName)
        val now = dateTimeProvider.instantNow()
        dao.initializeHousehold(
            profile = HouseholdProfileEntity(familyName = request.familyName, createdAt = now),
            firstMember = FamilyMemberEntity(name = request.firstMemberName, createdAt = now),
        )
    }

    override suspend fun upsertFamilyMember(input: FamilyMemberInput): Long {
        val member = PlannerInputNormalizer.familyMember(input)
        val existing = member.id?.let { dao.getFamilyMemberById(it) }
        val now = dateTimeProvider.instantNow()
        val id = dao.upsertFamilyMember(
            FamilyMemberEntity(
                id = member.id ?: 0,
                name = member.name,
                color = member.color ?: PlannerInputNormalizer.DEFAULT_MEMBER_COLOR,
                avatarUri = member.avatarUri,
                birthday = member.birthday,
                bio = member.bio,
                createdAt = existing?.createdAt ?: now,
            ),
        )

        val effectiveId = member.id ?: id
        dao.deleteBirthdayEventsForMember(effectiveId)
        return effectiveId
    }

    override suspend fun deleteFamilyMember(id: Long) {
        if (id <= 0) return
        dao.deleteFamilyMemberAndDetachReferences(
            memberId = id,
            neutralEventColor = PlannerInputNormalizer.DEFAULT_EVENT_COLOR,
        )
    }

    override suspend fun upsertEvent(input: EventInput): Long {
        val event = PlannerInputNormalizer.event(input)
        val existing = event.id?.let { dao.getEventById(it) }
        val id = dao.upsertEvent(
            PlannerEventEntity(
                id = event.id ?: 0,
                title = event.title,
                eventDate = event.eventDate,
                startTime = event.startTime,
                endTime = event.endTime,
                recurrenceType = event.recurrenceType?.storageValue,
                recurrenceUntil = event.recurrenceUntil,
                ownerId = event.ownerId,
                color = resolveEventColor(event.ownerId),
                note = event.note,
                sourceType = null,
                sourceMemberId = null,
                sourceYear = null,
                seriesStartDate = null,
                createdAt = existing?.createdAt ?: dateTimeProvider.instantNow(),
            ),
        )

        return event.id ?: id
    }

    override suspend fun deleteEvent(id: Long) {
        if (id > 0) dao.deleteEvent(id)
    }

    override suspend fun upsertMeal(input: MealPlanInput): Long {
        val meal = PlannerInputNormalizer.meal(input)
        val existing = meal.id?.let { dao.getMealById(it) }
        val id = dao.upsertMeal(
            MealPlanEntity(
                id = meal.id ?: 0,
                dayOfWeek = meal.dayOfWeek,
                mealType = meal.mealType,
                meal = meal.meal,
                ownerId = meal.ownerId,
                note = meal.note,
                createdAt = existing?.createdAt ?: dateTimeProvider.instantNow(),
            ),
        )

        return meal.id ?: id
    }

    override suspend fun deleteMeal(id: Long) {
        if (id > 0) dao.deleteMeal(id)
    }

    override suspend fun upsertBudgetMonth(input: BudgetMonthInput): Long {
        val budget = PlannerInputNormalizer.budgetMonth(input)
        val month = budget.month.toString()
        val existing = dao.getBudgetMonth(month)
        val now = dateTimeProvider.instantNow()
        val id = dao.upsertBudgetMonth(
            BudgetMonthEntity(
                id = budget.id ?: existing?.id ?: 0,
                month = month,
                limit = budget.limit,
                income = budget.income,
                currencyCode = budget.currencyCode,
                updatedAt = now,
                createdAt = existing?.createdAt ?: now,
            ),
        )

        return budget.id ?: existing?.id ?: id
    }

    override suspend fun upsertExpense(input: ExpenseInput): Long {
        val expense = PlannerInputNormalizer.expense(input)
        val existing = expense.id?.let { dao.getExpenseById(it) }
        val id = dao.upsertExpense(
            ExpenseEntity(
                id = expense.id ?: 0,
                amount = expense.amount,
                category = expense.category ?: PlannerInputNormalizer.DEFAULT_EXPENSE_CATEGORY,
                expenseDate = expense.expenseDate,
                ownerId = expense.ownerId,
                description = expense.description,
                month = expense.expenseDate.toString().take(7),
                createdAt = existing?.createdAt ?: dateTimeProvider.instantNow(),
            ),
        )

        return expense.id ?: id
    }

    override suspend fun deleteExpense(id: Long) {
        if (id > 0) dao.deleteExpense(id)
    }

    override suspend fun upsertNote(input: NoteInput): Long {
        val note = PlannerInputNormalizer.note(input)
        val existing = note.id?.let { dao.getNoteById(it) }
        val id = dao.upsertNote(
            NoteEntity(
                id = note.id ?: 0,
                title = note.title,
                ownerId = note.ownerId,
                content = note.content,
                createdAt = existing?.createdAt ?: dateTimeProvider.instantNow(),
            ),
        )

        return note.id ?: id
    }

    override suspend fun deleteNote(id: Long) {
        if (id > 0) dao.deleteNote(id)
    }

    override suspend fun upsertShoppingItem(input: ShoppingItemInput): Long {
        val item = PlannerInputNormalizer.shoppingItem(input)
        val existing = item.id?.let { dao.getShoppingItemById(it) }
        val id = dao.upsertShoppingItem(
            ShoppingItemEntity(
                id = item.id ?: 0,
                item = item.item,
                ownerId = item.ownerId,
                quantity = item.quantity,
                done = existing?.done ?: false,
                doneAt = existing?.doneAt,
                createdAt = existing?.createdAt ?: dateTimeProvider.instantNow(),
            ),
        )

        return item.id ?: id
    }

    override suspend fun toggleShoppingItem(id: Long) {
        if (id <= 0) return

        val item = dao.getShoppingItemById(id) ?: return
        val done = !item.done
        dao.setShoppingItemDone(
            id = id,
            done = done,
            doneAt = if (done) dateTimeProvider.instantNow() else null,
        )
    }

    override suspend fun deleteShoppingItem(id: Long) {
        if (id > 0) dao.deleteShoppingItem(id)
    }

    override suspend fun cleanupDoneShoppingItems() {
        val now = dateTimeProvider.instantNow()
        val cutoff = now.minus(Duration.ofSeconds(PlannerInputNormalizer.SHOPPING_DONE_RETENTION_SECONDS))
        dao.cleanupDoneShoppingItems(markedAt = now, cutoff = cutoff)
    }

    private suspend fun resolveEventColor(ownerId: Long?): String {
        if (ownerId == null) {
            return PlannerInputNormalizer.DEFAULT_EVENT_COLOR
        }

        return dao.getFamilyMemberById(ownerId)?.color
            ?.takeIf { it.isNotBlank() }
            ?: PlannerInputNormalizer.DEFAULT_EVENT_COLOR
    }
}
