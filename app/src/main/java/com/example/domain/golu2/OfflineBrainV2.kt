package com.example.domain.golu2

import java.util.Locale

data class OfflineBrainResult(
    val handled: Boolean,
    val reply: String,
    val capabilityNote: String,
)

/**
 * Feature 8: Advanced Offline Brain.
 * Performs strictly local math, unit conversions, Hindi/English phrasebook lookup,
 * and offline capability disclosure. Never pretends cloud LLM is running offline.
 */
object OfflineBrainV2 {

    private val commonPhrases =
        mapOf(
            "hello" to "नमस्ते (Namaste)",
            "thank you" to "धन्यवाद (Dhanyavaad)",
            "how are you" to "आप कैसे हैं? (Aap kaise hain?)",
            "good morning" to "सुप्रभात (Suprabhat)",
            "good night" to "शुभ रात्रि (Shubh Ratri)",
            "water" to "पानी (Paani)",
            "help" to "मदद (Madad)",
            "where is" to "कहाँ है (Kahan hai)",
        )

    fun evaluateLocalQuery(input: String): OfflineBrainResult {
        val q = input.trim()
        val lower = q.lowercase(Locale.ROOT)

        // 1. Offline Math Expression: e.g. "calculate 125 * 8" or "25 + 75"
        val mathMatch = Regex("(?:calculate\\s+)?(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(-?\\d+(?:\\.\\d+)?)").find(lower)
        if (mathMatch != null) {
            val a = mathMatch.groupValues[1].toDoubleOrNull()
            val op = mathMatch.groupValues[2]
            val b = mathMatch.groupValues[3].toDoubleOrNull()
            if (a != null && b != null) {
                val res =
                    when (op) {
                        "+" -> a + b
                        "-" -> a - b
                        "*" -> a * b
                        "/" -> if (b == 0.0) null else a / b
                        else -> null
                    }
                if (res != null) {
                    val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.4f", res)
                    return OfflineBrainResult(
                        handled = true,
                        reply = "Boss (Offline Calculator): $a $op $b = $formatted",
                        capabilityNote = "Computed 100% locally without network.",
                    )
                }
            }
        }

        // 2. Unit conversion: km -> miles, c -> f, kg -> lbs
        val unitMatch = Regex("(-?\\d+(?:\\.\\d+)?)\\s*(km|miles|c|f|kg|lbs)\\b").find(lower)
        if (unitMatch != null) {
            val valNum = unitMatch.groupValues[1].toDoubleOrNull()
            val unit = unitMatch.groupValues[2]
            if (valNum != null) {
                val converted =
                    when (unit) {
                        "km" -> String.format(Locale.US, "%.2f km = %.2f miles", valNum, valNum * 0.621371)
                        "miles" -> String.format(Locale.US, "%.2f miles = %.2f km", valNum, valNum / 0.621371)
                        "c" -> String.format(Locale.US, "%.1f °C = %.1f °F", valNum, (valNum * 9.0 / 5.0) + 32.0)
                        "f" -> String.format(Locale.US, "%.1f °F = %.1f °C", valNum, (valNum - 32.0) * 5.0 / 9.0)
                        "kg" -> String.format(Locale.US, "%.2f kg = %.2f lbs", valNum, valNum * 2.20462)
                        "lbs" -> String.format(Locale.US, "%.2f lbs = %.2f kg", valNum, valNum / 2.20462)
                        else -> ""
                    }
                if (converted.isNotBlank()) {
                    return OfflineBrainResult(
                        handled = true,
                        reply = "Boss (Offline Converter): $converted",
                        capabilityNote = "Converted locally on-device.",
                    )
                }
            }
        }

        // 3. Offline Hindi/English quick phrasebook
        commonPhrases.entries.firstOrNull { lower.contains(it.key) }?.let { (eng, hin) ->
            return OfflineBrainResult(
                handled = true,
                reply = "Offline Phrasebook: \"$eng\" → $hin",
                capabilityNote = "Local phrasebook match. Full AI multilingual translation requires Cloud backend.",
            )
        }

        return OfflineBrainResult(
            handled = false,
            reply =
                "Boss, yeh query Cloud AI ke bina solve nahi ho sakti. Offline mode mein time, date, battery, math, unit conversions, app launch, alarm, aur local voice recording available hain.",
            capabilityNote = "Honest offline boundary: Cloud AI requires backend connection.",
        )
    }
}
