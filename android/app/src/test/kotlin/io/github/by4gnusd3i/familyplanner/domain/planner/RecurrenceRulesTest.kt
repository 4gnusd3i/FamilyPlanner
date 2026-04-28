package io.github.by4gnusd3i.familyplanner.domain.planner

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
}
