package io.github.by4gnusd3i.familyplanner.data.local

import io.github.by4gnusd3i.familyplanner.domain.model.FamilyMember
import io.github.by4gnusd3i.familyplanner.domain.model.ExpenseItem
import io.github.by4gnusd3i.familyplanner.domain.model.MealPlan
import io.github.by4gnusd3i.familyplanner.domain.model.NoteItem
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerEvent
import io.github.by4gnusd3i.familyplanner.domain.model.RecurrenceType
import io.github.by4gnusd3i.familyplanner.domain.model.ShoppingItem

fun FamilyMemberEntity.toDomain(): FamilyMember =
    FamilyMember(
        id = id,
        name = name,
        color = color,
        avatarUri = avatarUri,
        birthday = birthday,
        bio = bio,
    )

fun PlannerEventEntity.toDomain(): PlannerEvent =
    PlannerEvent(
        id = id,
        title = title,
        eventDate = eventDate,
        startTime = startTime,
        endTime = endTime,
        recurrenceType = RecurrenceType.fromStorage(recurrenceType),
        recurrenceUntil = recurrenceUntil,
        ownerId = ownerId,
        color = color,
        note = note,
        sourceType = sourceType,
        sourceMemberId = sourceMemberId,
        sourceYear = sourceYear,
        seriesStartDate = seriesStartDate,
    )

fun MealPlanEntity.toDomain(): MealPlan =
    MealPlan(
        id = id,
        dayOfWeek = dayOfWeek,
        mealType = mealType,
        meal = meal,
        ownerId = ownerId,
        note = note,
    )

fun ExpenseEntity.toDomain(): ExpenseItem =
    ExpenseItem(
        id = id,
        amount = amount,
        category = category,
        expenseDate = expenseDate,
        ownerId = ownerId,
        description = description,
        month = month,
    )

fun NoteEntity.toDomain(): NoteItem =
    NoteItem(
        id = id,
        title = title,
        ownerId = ownerId,
        content = content,
    )

fun ShoppingItemEntity.toDomain(): ShoppingItem =
    ShoppingItem(
        id = id,
        item = item,
        ownerId = ownerId,
        quantity = quantity,
        done = done,
        doneAt = doneAt,
    )
