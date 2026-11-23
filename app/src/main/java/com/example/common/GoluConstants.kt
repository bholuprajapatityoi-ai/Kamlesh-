package com.example.common

object GoluConstants {
    const val APP_NAME = "GOLU"
    const val WAKE_PHRASE = "Hey Golu"
    const val DEFAULT_GREETING = "Hello! Boss, GOLU ready hai!"
    const val PRIMARY_USER_TITLE = "Boss"

    const val WAKE_WORD_CHANNEL_ID = "golu_wake_word_channel"
    const val WAKE_WORD_NOTIFICATION_ID = 4001

    val SUPPORTED_LANGUAGES = listOf("Hinglish", "Hindi", "English")
    val VOICE_GENDERS = listOf("Male", "Female")
    val RESPONSE_STYLES = listOf("Playful Boss Mode", "Concise", "Detailed")
}
