package com.hallysam.prefixcallblocker

import android.telephony.PhoneNumberUtils as AndroidPhoneNumberUtils
import java.util.Locale

object PhoneNumberUtils {

    fun normalizePhoneNumber(value: String): String {
        return AndroidPhoneNumberUtils.normalizeNumber(value).filter(Char::isDigit)
    }

    fun normalizePrefix(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null

        val cleaned = buildString {
            trimmed.forEachIndexed { index, char ->
                when {
                    char.isDigit() -> append(char)
                    char == '+' && index == 0 -> append('+')
                    char.isWhitespace() || char == '-' || char == '(' || char == ')' -> Unit
                    else -> return null
                }
            }
        }

        if (cleaned == "+" || cleaned.isEmpty()) return null
        val digits = cleaned.removePrefix("+")
        if (digits.isEmpty() || digits.length > 15) return null

        return if (cleaned.startsWith("+")) "+$digits" else digits
    }

    fun matchesPrefix(incomingNumber: String, savedPrefixes: Set<String>): String? {
        val incomingDigits = normalizePhoneNumber(incomingNumber)
        if (incomingDigits.isEmpty() || savedPrefixes.isEmpty()) return null

        val candidates = linkedSetOf(incomingDigits)

        // Nigerian-friendly local/international equivalence:
        // 0700... <-> +234700...
        if (incomingDigits.startsWith("234") && incomingDigits.length > 3) {
            candidates += "0" + incomingDigits.removePrefix("234")
        }

        for (prefix in savedPrefixes) {
            val normalizedPrefix = normalizePrefix(prefix) ?: continue
            val prefixDigits = normalizedPrefix.removePrefix("+")
            if (candidates.any { it.startsWith(prefixDigits) }) {
                return normalizedPrefix
            }
        }

        return null
    }

    fun displayNumber(value: String): String {
        val digits = normalizePhoneNumber(value)
        return if (digits.isEmpty()) value else digits
    }

    fun countryAwareLabel(prefix: String): String {
        return prefix.lowercase(Locale.ROOT)
    }
}
