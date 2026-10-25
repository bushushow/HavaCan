package com.example.weather.domain.voice

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Small Turkish text helpers for voice commands and spoken replies. Pure Kotlin. */
object TurkishText {
    val TR: Locale = Locale.forLanguageTag("tr")

    private val FOLD = mapOf('ı' to 'i', 'ş' to 's', 'ğ' to 'g', 'ü' to 'u', 'ö' to 'o', 'ç' to 'c', 'â' to 'a', 'î' to 'i', 'û' to 'u')
    private val BACK_VOWELS = setOf('a', 'ı', 'o', 'u')
    private val VOWELS = setOf('a', 'e', 'ı', 'i', 'o', 'ö', 'u', 'ü')
    private val VOICELESS = setOf('ç', 'f', 'h', 'k', 'p', 's', 'ş', 't')

    /** Lowercase Turkish, apostrophes split ("6'da" → "6 da"), punctuation dropped. Keeps ':' and '.' inside times. */
    fun clean(text: String): String = text
        .lowercase(TR)
        .replace(Regex("[’'`´]"), " ")
        .replace(Regex("(?<!\\d)[.:]|[.:](?!\\d)"), " ")
        .replace(Regex("[^\\p{L}\\p{N}:. ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    /** [clean] plus Turkish letters folded to ASCII, so "dolabıma" and "dolabima" match alike. */
    fun fold(text: String): String = clean(text).map { FOLD[it] ?: it }.joinToString("")

    fun foldWord(word: String): String = word.lowercase(TR).map { FOLD[it] ?: it }.joinToString("")

    /** "Ankara" → "Ankara'da", "Kadıköy" → "Kadıköy'de", "Sinop" → "Sinop'ta". */
    fun locative(name: String): String {
        val lower = name.lowercase(TR)
        val lastVowel = lower.lastOrNull { it in VOWELS } ?: 'e'
        val vowel = if (lastVowel in BACK_VOWELS) 'a' else 'e'
        val consonant = if (lower.lastOrNull() in VOICELESS) 't' else 'd'
        return "$name'$consonant$vowel"
    }

    /** "bugün", "yarın", a weekday this week ("Cumartesi") or "3 Kasım". */
    fun dayPhrase(date: LocalDate, today: LocalDate): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
        return when {
            days == 0L -> "bugün"
            days == 1L -> "yarın"
            days in 2..6 -> date.dayOfWeek.getDisplayName(TextStyle.FULL, TR).replaceFirstChar { it.titlecase(TR) }
            else -> "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, TR).replaceFirstChar { it.titlecase(TR) }}"
        }
    }

    /** "16°" → "16 derece", "%40" → "yüzde 40": text the Turkish TTS voice reads naturally. */
    fun forSpeech(text: String): String = text
        .replace(Regex("%\\s?(\\d+)"), "yüzde $1")
        .replace(Regex("(-?\\d+)\\s?°"), "$1 derece")
        .replace("km/sa", "kilometre")
        .replace(Regex("\\s*·\\s*"), ", ")
        .replace(Regex("[“”\"]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun capitalize(text: String): String = text.replaceFirstChar { it.titlecase(TR) }
}
