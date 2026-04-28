package io.github.by4gnusd3i.familyplanner.domain.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SetupValidatorTest {
    @Test
    fun trimsValidSetupValues() {
        val request = SetupValidator.validate("  Hansen  ", "  Anna  ")

        assertEquals("Hansen", request.familyName)
        assertEquals("Anna", request.firstMemberName)
    }

    @Test
    fun rejectsMissingFamilyName() {
        assertThrows(IllegalArgumentException::class.java) {
            SetupValidator.validate(" ", "Anna")
        }
    }

    @Test
    fun rejectsMissingFirstMemberName() {
        assertThrows(IllegalArgumentException::class.java) {
            SetupValidator.validate("Hansen", " ")
        }
    }
}
