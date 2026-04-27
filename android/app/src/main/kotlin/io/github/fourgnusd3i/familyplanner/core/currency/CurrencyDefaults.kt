package io.github.fourgnusd3i.familyplanner.core.currency

import java.util.Currency
import java.util.Locale

object CurrencyDefaults {
    fun defaultCurrencyCode(locale: Locale): String {
        val country = locale.country
        if (country.isNotBlank()) {
            runCatching { Currency.getInstance(locale).currencyCode }
                .getOrNull()
                ?.let { return it }
        }

        return when (locale.language.lowercase(Locale.ROOT)) {
            "nb", "nn", "no" -> "NOK"
            else -> "USD"
        }
    }
}
