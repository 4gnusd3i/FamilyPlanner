package io.github.by4gnusd3i.familyplanner.data.local

import io.github.by4gnusd3i.familyplanner.domain.model.FamilyMember
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
