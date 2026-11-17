package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "room_management_prefs")

data class RememberedSession(
    val userId: String,
    val sessionToken: String
)

class SessionPreferences(private val context: Context) {

    private object Keys {
        val REMEMBERED_USER_ID = stringPreferencesKey("remembered_user_id")
        val REMEMBERED_TOKEN = stringPreferencesKey("remembered_token")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val code = prefs[Keys.LANGUAGE_CODE] ?: AppLanguage.ENGLISH.code
        AppLanguage.entries.firstOrNull { it.code == code } ?: AppLanguage.ENGLISH
    }

    val currencySymbolFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENCY_SYMBOL] ?: "৳"
    }

    suspend fun saveRememberedSession(userId: String, sessionToken: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMEMBERED_USER_ID] = userId
            prefs[Keys.REMEMBERED_TOKEN] = sessionToken
        }
    }

    suspend fun getRememberedSession(): RememberedSession? {
        val prefs = context.dataStore.data.first()
        val uid = prefs[Keys.REMEMBERED_USER_ID]
        val token = prefs[Keys.REMEMBERED_TOKEN]
        return if (!uid.isNullOrBlank() && !token.isNullOrBlank()) {
            RememberedSession(uid, token)
        } else {
            null
        }
    }

    suspend fun clearRememberedSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.REMEMBERED_USER_ID)
            prefs.remove(Keys.REMEMBERED_TOKEN)
        }
    }

    suspend fun clearRememberedSessionIfMatches(userId: String) {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.REMEMBERED_USER_ID] == userId) {
                prefs.remove(Keys.REMEMBERED_USER_ID)
                prefs.remove(Keys.REMEMBERED_TOKEN)
            }
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_SYMBOL] = symbol
        }
    }
}
