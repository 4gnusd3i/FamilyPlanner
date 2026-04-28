package io.github.by4gnusd3i.familyplanner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.by4gnusd3i.familyplanner.core.currency.CurrencyDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "familyplanner_settings")

data class AppSettings(
    val languageOverride: String?,
    val currencyCode: String,
)

@Singleton
class AppSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val settings: Flow<AppSettings> =
        context.settingsDataStore.data.map { preferences ->
            AppSettings(
                languageOverride = preferences[Keys.LanguageOverride],
                currencyCode = preferences[Keys.CurrencyCode]
                    ?: CurrencyDefaults.defaultCurrencyCode(Locale.getDefault()),
            )
        }

    suspend fun setLanguageOverride(languageId: String?) {
        context.settingsDataStore.edit { preferences ->
            if (languageId.isNullOrBlank()) {
                preferences.remove(Keys.LanguageOverride)
            } else {
                preferences[Keys.LanguageOverride] = languageId
            }
        }
    }

    suspend fun setCurrencyCode(currencyCode: String) {
        val normalized = currencyCode.trim().uppercase(Locale.ROOT)
        require(normalized.length == 3 && normalized.all { it in 'A'..'Z' }) {
            "currency code must be three letters"
        }

        context.settingsDataStore.edit { preferences ->
            preferences[Keys.CurrencyCode] = normalized
        }
    }

    private object Keys {
        val LanguageOverride = stringPreferencesKey("language_override")
        val CurrencyCode = stringPreferencesKey("currency_code")
    }
}
