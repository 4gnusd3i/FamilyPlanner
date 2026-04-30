package io.github.by4gnusd3i.familyplanner.domain.planner

import io.github.by4gnusd3i.familyplanner.domain.model.FamilyMember
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerEvent
import io.github.by4gnusd3i.familyplanner.domain.model.RecurrenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object RecurrenceRules {
    private const val UPCOMING_WINDOW_DAYS = 2L
    const val BIRTHDAY_SOURCE_TYPE: String = "birthday"
    const val RECURRING_SOURCE_TYPE: String = "recurring"

    fun expandRecurringEvents(
        events: List<PlannerEvent>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<PlannerEvent> {
        if (rangeEnd < rangeStart) return emptyList()

        return events
            .filter { it.recurrenceType != null }
            .flatMap { series -> expandSeries(series, rangeStart, rangeEnd) }
            .sortedWith(eventComparator())
    }

    fun upcomingEvents(
        events: List<PlannerEvent>,
        now: LocalDateTime,
    ): List<PlannerEvent> {
        val fromDate = now.toLocalDate()
        val windowEnd = fromDate.plusDays(UPCOMING_WINDOW_DAYS)
        val directEvents = events
            .filter { it.recurrenceType == null }
            .filter { isUpcoming(it, now, windowEnd) }

        val recurringEvents = expandRecurringEvents(events, fromDate, windowEnd)
            .filter { isUpcoming(it, now, windowEnd) }
            .groupBy { it.id }
            .map { (_, occurrences) -> occurrences.sortedWith(eventComparator()).first() }

        return (directEvents + recurringEvents).sortedWith(eventComparator())
    }

    fun generatedBirthdayEvents(
        familyMembers: List<FamilyMember>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<PlannerEvent> {
        if (rangeEnd < rangeStart) return emptyList()

        return familyMembers
            .flatMap { member -> birthdayEventsForMember(member, rangeStart, rangeEnd) }
            .sortedWith(eventComparator())
    }

    fun birthdayDateForYear(birthday: LocalDate, year: Int): LocalDate =
        if (birthday.monthValue == 2 && birthday.dayOfMonth == 29 && !java.time.Year.isLeap(year.toLong())) {
            LocalDate.of(year, 2, 28)
        } else {
            LocalDate.of(year, birthday.monthValue, birthday.dayOfMonth)
        }

    private fun birthdayEventsForMember(
        member: FamilyMember,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<PlannerEvent> {
        val birthday = member.birthday ?: return emptyList()
        return (rangeStart.year..rangeEnd.year).mapNotNull { year ->
            val birthdayDate = birthdayDateForYear(birthday, year)
            if (birthdayDate < rangeStart || birthdayDate > rangeEnd) {
                null
            } else {
                PlannerEvent(
                    id = birthdayEventId(member.id, year),
                    title = member.name,
                    eventDate = birthdayDate,
                    ownerId = member.id,
                    color = member.color,
                    sourceType = BIRTHDAY_SOURCE_TYPE,
                    sourceMemberId = member.id,
                    sourceYear = year,
                )
            }
        }
    }

    private fun birthdayEventId(memberId: Long, year: Int): Long =
        -((memberId.coerceAtLeast(1L) * 10_000L) + year)

    private fun expandSeries(
        series: PlannerEvent,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<PlannerEvent> {
        val recurrenceType = series.recurrenceType ?: return emptyList()
        val recurrenceEnd = series.recurrenceUntil ?: rangeEnd
        if (recurrenceEnd < rangeStart || recurrenceEnd < series.eventDate) return emptyList()

        val effectiveEnd = minOf(recurrenceEnd, rangeEnd)
        val occurrences = mutableListOf<PlannerEvent>()
        var occurrence = firstOccurrenceOnOrAfter(series.eventDate, recurrenceType, rangeStart)
        while (occurrence <= effectiveEnd) {
            if (occurrence >= series.eventDate) {
                occurrences += series.copy(
                    eventDate = occurrence,
                    sourceType = RECURRING_SOURCE_TYPE,
                    seriesStartDate = series.eventDate,
                )
            }
            occurrence = nextOccurrence(occurrence, recurrenceType)
        }
        return occurrences
    }

    private fun firstOccurrenceOnOrAfter(
        seriesStart: LocalDate,
        recurrenceType: RecurrenceType,
        rangeStart: LocalDate,
    ): LocalDate {
        if (rangeStart <= seriesStart) return seriesStart
        return when (recurrenceType) {
            RecurrenceType.Daily -> rangeStart
            RecurrenceType.Weekly -> {
                val daysBetween = rangeStart.toEpochDay() - seriesStart.toEpochDay()
                seriesStart.plusDays(((daysBetween + 6) / 7) * 7)
            }
        }
    }

    private fun nextOccurrence(date: LocalDate, recurrenceType: RecurrenceType): LocalDate =
        when (recurrenceType) {
            RecurrenceType.Daily -> date.plusDays(1)
            RecurrenceType.Weekly -> date.plusWeeks(1)
        }

    private fun isUpcoming(event: PlannerEvent, now: LocalDateTime, windowEnd: LocalDate): Boolean {
        if (event.eventDate > windowEnd) return false
        if (event.eventDate > now.toLocalDate()) return true
        if (event.eventDate < now.toLocalDate()) return false

        val cutoff = event.endTime ?: event.startTime ?: return true
        return cutoff >= now.toLocalTime()
    }

    private fun eventComparator(): Comparator<PlannerEvent> =
        compareBy<PlannerEvent> { it.eventDate }
            .thenBy { it.endTime ?: it.startTime ?: LocalTime.MAX }
            .thenBy { it.title }
}
