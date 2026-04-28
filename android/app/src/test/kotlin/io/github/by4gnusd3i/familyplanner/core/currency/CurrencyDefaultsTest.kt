package io.github.by4gnusd3i.familyplanner.core.currency

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CurrencyDefaultsTest {
    @Test
    fun norwegianLanguageWithoutCountryDefaultsToNok() {
        assertEquals("NOK", CurrencyDefaults.defaultCurrencyCode(Locale.forLanguageTag("nb")))
    }

    @Test
    fun unsupportedLanguageWithoutCountryDefaultsToUsd() {
        assertEquals("USD", CurrencyDefaults.defaultCurrencyCode(Locale.forLanguageTag("es")))
    }

    @Test
    fun localeCountryCurrencyWinsWhenAvailable() {
        assertEquals("EUR", CurrencyDefaults.defaultCurrencyCode(Locale.forLanguageTag("en-IE")))
    }
}
