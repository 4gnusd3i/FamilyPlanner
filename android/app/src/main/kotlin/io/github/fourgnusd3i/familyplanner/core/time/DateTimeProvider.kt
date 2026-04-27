package io.github.fourgnusd3i.familyplanner.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

interface DateTimeProvider {
    fun today(): LocalDate
    fun now(): LocalDateTime
    fun instantNow(): Instant

    object System : DateTimeProvider {
        override fun today(): LocalDate = LocalDate.now()
        override fun now(): LocalDateTime = LocalDateTime.now()
        override fun instantNow(): Instant = Instant.now()
    }
}
