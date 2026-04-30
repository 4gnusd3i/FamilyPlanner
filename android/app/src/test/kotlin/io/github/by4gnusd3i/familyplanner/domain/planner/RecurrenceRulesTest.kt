package io.github.by4gnusd3i.familyplanner.domain.planner

import io.github.by4gnusd3i.familyplanner.domain.model.FamilyMember
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerEvent
import io.github.by4gnusd3i.familyplanner.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RecurrenceRulesTest {
    @Test
    fun weeklySeriesExpandsOnlyInsideRange() {
        val series = PlannerEvent(
            id = 7,
            title = "Training",
            eventDate = LocalDate.of(2026, 4, 6),
            recurrenceType = RecurrenceType.Weekly,
            recurrenceUntil = LocalDate.of(2026, 4, 30),
        )

        val occurrences = RecurrenceRules.expandRecurringEvents(
            events = listOf(series),
            rangeStart = LocalDate.of(2026, 4, 20),
            rangeEnd = LocalDate.of(2026, 4, 26),
        )

        assertEquals(listOf(LocalDate.of(2026, 4, 20)), occurrences.map { it.eventDate })
        assertEquals(RecurrenceRules.RECURRING_SOURCE_TYPE, occurrences.single().sourceType)
        assertEquals(LocalDate.of(2026, 4, 6), occurrences.single().seriesStartDate)
    }

    @Test
    fun upcomingKeepsOnlyNextOccurrencePerRecurringSeries() {
        val series = PlannerEvent(
            id = 42,
            title = "Daily reading",
            eventDate = LocalDate.of(2026, 4, 20),
            startTime = LocalTime.of(18, 0),
            recurrenceType = RecurrenceType.Daily,
            recurrenceUntil = LocalDate.of(2026, 4, 30),
        )

        val upcoming = RecurrenceRules.upcomingEvents(
            events = listOf(series),
            now = LocalDateTime.of(2026, 4, 22, 12, 0),
        )

        assertEquals(1, upcoming.size)
        assertEquals(LocalDate.of(2026, 4, 22), upcoming.single().eventDate)
    }

    @Test
    fun eventsInRangeIncludesDirectAndRecurringOccurrences() {
        val direct = PlannerEvent(
            id = 1,
            title = "Dentist",
            eventDate = LocalDate.of(2026, 4, 23),
        )
        val recurring = PlannerEvent(
            id = 2,
            title = "Reading",
            eventDate = LocalDate.of(2026, 4, 20),
            recurrenceType = RecurrenceType.Daily,
            recurrenceUntil = LocalDate.of(2026, 4, 25),
        )

        val events = RecurrenceRules.eventsInRange(
            events = listOf(direct, recurring),
            rangeStart = LocalDate.of(2026, 4, 22),
            rangeEnd = LocalDate.of(2026, 4, 24),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 4, 22),
                LocalDate.of(2026, 4, 23),
                LocalDate.of(2026, 4, 23),
                LocalDate.of(2026, 4, 24),
            ),
            events.map { it.eventDate },
        )
        assertEquals(1, events.count { it.sourceType == null })
        assertEquals(3, events.count { it.sourceType == RecurrenceRules.RECURRING_SOURCE_TYPE })
    }

    @Test
    fun sameDayUntimedEventRemainsUpcomingUntilDayPasses() {
        val event = PlannerEvent(
            id = 1,
            title = "Remember backpack",
            eventDate = LocalDate.of(2026, 4, 22),
        )

        val upcoming = RecurrenceRules.upcomingEvents(
            events = listOf(event),
            now = LocalDateTime.of(2026, 4, 22, 23, 59),
        )

        assertEquals(listOf(event), upcoming)
    }

    @Test
    fun leapDayBirthdayUsesFebruary28InNonLeapYears() {
        val birthday = LocalDate.of(2024, 2, 29)

        assertEquals(
            LocalDate.of(2026, 2, 28),
            RecurrenceRules.birthdayDateForYear(birthday, 2026),
        )
        assertEquals(
            LocalDate.of(2028, 2, 29),
            RecurrenceRules.birthdayDateForYear(birthday, 2028),
        )
    }

    @Test
    fun generatedBirthdayEventsOnlyIncludeBirthdaysInsideRange() {
        val anna = familyMember(
            id = 4,
            name = "Anna",
            birthday = LocalDate.of(2018, 4, 23),
        )
        val magnus = familyMember(
            id = 5,
            name = "Magnus",
            birthday = LocalDate.of(2015, 4, 26),
        )

        val birthdays = RecurrenceRules.generatedBirthdayEvents(
            familyMembers = listOf(anna, magnus),
            rangeStart = LocalDate.of(2026, 4, 22),
            rangeEnd = LocalDate.of(2026, 4, 24),
        )

        assertEquals(1, birthdays.size)
        birthdays.single().let { event ->
            assertEquals("Anna", event.title)
            assertEquals(LocalDate.of(2026, 4, 23), event.eventDate)
            assertEquals(4L, event.ownerId)
            assertEquals("#4F83F1", event.color)
            assertEquals(RecurrenceRules.BIRTHDAY_SOURCE_TYPE, event.sourceType)
            assertEquals(4L, event.sourceMemberId)
            assertEquals(2026, event.sourceYear)
        }
    }

    @Test
    fun generatedLeapDayBirthdayUsesFebruary28InNonLeapYears() {
        val member = familyMember(
            id = 9,
            name = "Leap",
            birthday = LocalDate.of(2024, 2, 29),
        )

        val birthdays = RecurrenceRules.generatedBirthdayEvents(
            familyMembers = listOf(member),
            rangeStart = LocalDate.of(2026, 2, 28),
            rangeEnd = LocalDate.of(2026, 2, 28),
        )

        assertEquals(listOf(LocalDate.of(2026, 2, 28)), birthdays.map { it.eventDate })
    }

    @Test
    fun upcomingIncludesGeneratedBirthdayUntilCalendarDayPasses() {
        val birthday = RecurrenceRules.generatedBirthdayEvents(
            familyMembers = listOf(
                familyMember(
                    id = 12,
                    name = "Nora",
                    birthday = LocalDate.of(2017, 4, 22),
                ),
            ),
            rangeStart = LocalDate.of(2026, 4, 22),
            rangeEnd = LocalDate.of(2026, 4, 24),
        )

        val upcoming = RecurrenceRules.upcomingEvents(
            events = birthday,
            now = LocalDateTime.of(2026, 4, 22, 23, 59),
        )

        assertEquals(listOf(LocalDate.of(2026, 4, 22)), upcoming.map { it.eventDate })
        assertEquals(RecurrenceRules.BIRTHDAY_SOURCE_TYPE, upcoming.single().sourceType)
    }

    private fun familyMember(
        id: Long,
        name: String,
        birthday: LocalDate?,
    ): FamilyMember =
        FamilyMember(
            id = id,
            name = name,
            color = "#4F83F1",
            avatarUri = null,
            birthday = birthday,
            bio = null,
        )
}
