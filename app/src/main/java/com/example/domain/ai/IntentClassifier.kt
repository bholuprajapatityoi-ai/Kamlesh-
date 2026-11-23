package com.example.domain.ai

import com.example.domain.model.IntentType

object IntentClassifier {

    fun classify(rawText: String, hasPriorContext: Boolean = false): IntentType {
        val t = rawText.lowercase().trim()
        if (t.isBlank()) return IntentType.GENERAL_CHAT

        // Phase 7 explicit permanent memory phrase
        if (containsAny(t, "ye yaad rakhna", "yaad rakhna", "remember this", "remember that")) {
            return IntentType.MEMORY_SAVE
        }

        // Phase 10 Daily life queries
        if (containsAny(t, "timetable", "daily schedule", "task list", "next reminder", "morning summary", "evening summary")) {
            return IntentType.DAILY_LIFE_QUERY
        }

        // Sensitive actions requiring explicit Boss confirmation
        if (containsAny(t, "delete ", "erase ", "factory reset", "transfer money", "pay ", "send email", "send message to")) {
            return IntentType.SENSITIVE_ACTION
        }

        if (containsAny(t, "whatsapp", "watsapp", "compose a message", "reply to a message")) {
            return IntentType.WHATSAPP_ACTION
        }
        if (containsAny(t, "call ", "phone ", "dial ", "call lagao", "phone karo")) {
            return IntentType.CALL_CONTACT
        }
        if (containsAny(t, "volume", "mute", "unmute", "pause music", "play music", "next song", "previous song", "wifi", "wi-fi", "bluetooth", "display setting", "sound setting", "phone info")) {
            return IntentType.PHONE_CONTROL
        }
        if (containsAny(t, "alarm", "subah ", "uthana", "wake me")) {
            return IntentType.SET_ALARM
        }
        if (containsAny(t, "reminder", "remind me", "yaad dilana", "calendar", "event", "meeting")) {
            return IntentType.SET_REMINDER
        }
        if (containsAny(t, "kholo", "open ", "launch ", "start ") &&
            !containsAny(t, "explain", "samjhao", "kya hai", "what is")
        ) {
            return IntentType.OPEN_APP
        }
        if (containsAny(t, "settings", "setting kholo")) {
            return IntentType.OPEN_SETTINGS
        }
        if (containsAny(t, "battery", "charge", "charging", "kitni battery")) {
            return IntentType.BATTERY_QUERY
        }
        if (containsAny(t, "time", "baje", "samay", "time kya", "kitna baja")) {
            return IntentType.TIME_QUERY
        }
        if (containsAny(t, "date", "tarikh", "aaj ka din", "today", "kaun sa din")) {
            return IntentType.DATE_QUERY
        }
        if (containsAny(t, "hello", "hi ", "hey golu", "namaste", "kaise ho", "kya haal", "good morning", "good evening") ||
            t == "hi" ||
            t == "hey" ||
            t == "golu"
        ) {
            return IntentType.GREETING
        }
        if (containsAny(t, "samjhao", "explain", "what is", "how does", "kyu", "kaise kaam", "define")) {
            return IntentType.EXPLANATION
        }
        if (hasPriorContext &&
            containsAny(t, "aur ", "more", "why", "example", "usse", "iska", "uske", "continue", "short mein")
        ) {
            return IntentType.FOLLOW_UP
        }
        return IntentType.GENERAL_CHAT
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any { text.contains(it) }
}
