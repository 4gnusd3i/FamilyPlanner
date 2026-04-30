package io.github.by4gnusd3i.familyplanner.data.repository

import io.github.by4gnusd3i.familyplanner.core.time.DateTimeProvider
import io.github.by4gnusd3i.familyplanner.data.avatar.AvatarStorageService
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
import io.github.by4gnusd3i.familyplanner.data.settings.AppSettingsRepository
import io.github.by4gnusd3i.familyplanner.domain.model.BudgetSnapshot
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Duration
import java.time.YearMonth
import javax.inject.Inject

class RoomPlannerRepository @Inject constructor(
    private val dao: PlannerDao,
    private val dateTimeProvider: DateTimeProvider,
    private val settingsRepository: AppSettingsRepository,
    private val avatarStorage: AvatarStorageService,
) : PlannerRepository {
    override fun observeSettings() = settingsRepository.settings

    override suspend fun setLanguageOverride(languageId: String?) {
        settingsRepository.setLanguageOverride(languageId)
    }

    override suspend fun setCurrencyCode(currencyCode: String) {
        settingsRepository.setCurrencyCode(currencyCode)
    }

    override fun observeDashboard(): Flow<PlannerDashboard> =
        combineCoreDashboard(
            dao.observeHouseholdCount(),
            dao.observeFamilyMembers(),
            dao.observeAllEvents(),
            dao.observeShoppingItems(),
            dao.observeNotes(),
        ).let { core ->
            val month = YearMonth.from(dateTimeProvider.today()).toString()
            combine(
                core,
                dao.observeMeals(),
                dao.observeBudgetMonth(month),
                dao.observeExpenses(month),
                settingsRepository.settings,
            ) { coreDashboard, meals, budgetMonth, expenses, settings ->
                val expenseItems = expenses.map { it.toDomain() }
                val spent = expenseItems.fold(BigDecimal.ZERO) { total, expense -> total + expense.amount }
                val limit = budgetMonth?.limit ?: BigDecimal.ZERO
                val income = budgetMonth?.income ?: BigDecimal.ZERO

                coreDashboard.copy(
                    meals = meals.map { it.toDomain() },
                    budget = BudgetSnapshot(
                        month = month,
                        limit = limit,
                        income = income,
                        spent = spent,
                        remaining = limit - spent,
                        available = income - spent,
                        currencyCode = budgetMonth?.currencyCode ?: settings.currencyCode,
                        expenses = expenseItems,
                    ),
                )
            }
        }

    private fun combineCoreDashboard(
        householdCount: Flow<Int>,
        familyMembers: Flow<List<FamilyMemberEntity>>,
        events: Flow<List<PlannerEventEntity>>,
        shoppingItems: Flow<List<ShoppingItemEntity>>,
        notes: Flow<List<NoteEntity>>,
    ): Flow<PlannerDashboard> =
        combine(
            householdCount,
            familyMembers,
            events,
            shoppingItems,
            notes,
        ) { householdProfileCount, family, plannerEvents, shopping, noteItems ->
            val now = dateTimeProvider.now()
            val today = now.toLocalDate()
            val weekStart = today.startOfWeek()
            val weekEnd = weekStart.plusDays(6)
            val familyDomain = family.map { it.toDomain() }
            val storedEvents = plannerEvents.map { it.toDomain() }
            val upcomingBirthdayEvents = RecurrenceRules.generatedBirthdayEvents(
                familyMembers = familyDomain,
                rangeStart = today,
                rangeEnd = today.plusDays(2),
            )
            val weekBirthdayEvents = RecurrenceRules.generatedBirthdayEvents(
                familyMembers = familyDomain,
                rangeStart = weekStart,
                rangeEnd = weekEnd,
            )
            val weekEvents = RecurrenceRules.eventsInRange(
                events = storedEvents + weekBirthdayEvents,
                rangeStart = weekStart,
                rangeEnd = weekEnd,
            )
            val upcoming = RecurrenceRules.upcomingEvents(
                events = storedEvents + upcomingBirthdayEvents,
                now = now,
            )
            PlannerDashboard(
                isSetupComplete = householdProfileCount > 0,
                familyMembers = familyDomain,
                weekStart = weekStart,
                weekEvents = weekEvents,
                upcomingEvents = upcoming,
                meals = emptyList(),
                budget = emptyBudgetSnapshot(),
                shoppingItems = shopping.map { it.toDomain() },
                notes = noteItems.map { it.toDomain() },
            )
        }

    private fun emptyBudgetSnapshot(): BudgetSnapshot =
        BudgetSnapshot(
            month = YearMonth.from(dateTimeProvider.today()).toString(),
            limit = BigDecimal.ZERO,
            income = BigDecimal.ZERO,
            spent = BigDecimal.ZERO,
            remaining = BigDecimal.ZERO,
            available = BigDecimal.ZERO,
            currencyCode = "USD",
            expenses = emptyList(),
        )

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
        val avatarUri = member.avatarUri
            ?.let { avatarStorage.storeAvatar(it) }
            ?: existing?.avatarUri
        val id = dao.upsertFamilyMember(
            FamilyMemberEntity(
                id = member.id ?: 0,
                name = member.name,
                color = member.color ?: PlannerInputNormalizer.DEFAULT_MEMBER_COLOR,
                avatarUri = avatarUri,
                birthday = member.birthday,
                bio = member.bio,
                createdAt = existing?.createdAt ?: now,
            ),
        )

        if (existing?.avatarUri != null && avatarUri != existing.avatarUri) {
            avatarStorage.deleteStoredAvatar(existing.avatarUri)
        }

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

    override suspend fun resetAllData() {
        dao.resetAll()
        avatarStorage.deleteAllStoredAvatars()
    }

    private suspend fun resolveEventColor(ownerId: Long?): String {
        if (ownerId == null) {
            return PlannerInputNormalizer.DEFAULT_EVENT_COLOR
        }

        return dao.getFamilyMemberById(ownerId)?.color
            ?.takeIf { it.isNotBlank() }
            ?: PlannerInputNormalizer.DEFAULT_EVENT_COLOR
    }

    private fun LocalDate.startOfWeek(): LocalDate =
        minusDays((dayOfWeek.value - 1).toLong())
}
