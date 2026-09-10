package com.hallysam.prefixcallblocker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prefixDataStore by preferencesDataStore(name = "prefix_blocker_settings")

data class BlockedCall(
    val number: String,
    val prefix: String,
    val timestamp: Long
)

class PrefixRepository(private val context: Context) {

    companion object {
        private val BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
        private val PREFIXES = stringSetPreferencesKey("prefixes")
        private val HISTORY = stringSetPreferencesKey("blocked_history")
    }

    val blockingEnabled: Flow<Boolean> =
        context.prefixDataStore.data.map { it[BLOCKING_ENABLED] ?: true }

    val prefixes: Flow<Set<String>> =
        context.prefixDataStore.data.map { it[PREFIXES] ?: emptySet() }

    val history: Flow<List<BlockedCall>> =
        context.prefixDataStore.data.map { prefs ->
            prefs[HISTORY].orEmpty()
                .mapNotNull(::decodeHistory)
                .sortedByDescending { it.timestamp }
        }

    suspend fun isBlockingEnabled(): Boolean =
        context.prefixDataStore.data.first()[BLOCKING_ENABLED] ?: true

    suspend fun getPrefixes(): Set<String> =
        context.prefixDataStore.data.first()[PREFIXES] ?: emptySet()

    suspend fun setBlockingEnabled(enabled: Boolean) {
        context.prefixDataStore.edit { it[BLOCKING_ENABLED] = enabled }
    }

    suspend fun addPrefix(prefix: String): Boolean {
        val normalized = PhoneNumberUtils.normalizePrefix(prefix) ?: return false
        context.prefixDataStore.edit { prefs ->
            val updated = prefs[PREFIXES].orEmpty().toMutableSet()
            updated.add(normalized)
            prefs[PREFIXES] = updated
        }
        return true
    }

    suspend fun deletePrefix(prefix: String) {
        context.prefixDataStore.edit { prefs ->
            val updated = prefs[PREFIXES].orEmpty().toMutableSet()
            updated.remove(prefix)
            prefs[PREFIXES] = updated
        }
    }

    suspend fun addBlockedCall(number: String, prefix: String) {
        val item = encodeHistory(
            BlockedCall(
                number = PhoneNumberUtils.displayNumber(number),
                prefix = prefix,
                timestamp = System.currentTimeMillis()
            )
        )
        context.prefixDataStore.edit { prefs ->
            val updated = prefs[HISTORY].orEmpty().toMutableSet()
            updated.add(item)
            // Keep local history bounded.
            val trimmed = updated
                .mapNotNull(::decodeHistory)
                .sortedByDescending { it.timestamp }
                .take(100)
                .map(::encodeHistory)
                .toSet()
            prefs[HISTORY] = trimmed
        }
    }

    suspend fun clearHistory() {
        context.prefixDataStore.edit { it[HISTORY] = emptySet() }
    }

    private fun encodeHistory(call: BlockedCall): String =
        listOf(
            call.timestamp.toString(),
            call.number.replace("|", ""),
            call.prefix.replace("|", "")
        ).joinToString("|")

    private fun decodeHistory(value: String): BlockedCall? {
        val parts = value.split("|", limit = 3)
        if (parts.size != 3) return null
        return parts[0].toLongOrNull()?.let {
            BlockedCall(parts[1], parts[2], it)
        }
    }
}
