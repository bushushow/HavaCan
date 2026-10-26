package com.kampplus.havacan.data

import java.util.Locale

data class Country(val code: String, val name: String) {
    /** Regional-indicator flag emoji, e.g. "TR" → 🇹🇷. */
    val flag: String
        get() = code.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")
}

const val TURKIYE = "TR"

/** All ISO 3166 countries with Turkish names, Türkiye first, the rest in Turkish alphabetical order. */
object Countries {
    private val turkish = Locale.forLanguageTag("tr")

    val all: List<Country> by lazy {
        val countries = Locale.getISOCountries().map { code ->
            Country(code, Locale.Builder().setRegion(code).build().getDisplayCountry(turkish))
        }
        val (home, others) = countries.partition { it.code == TURKIYE }
        home + others.sortedWith(compareBy(turkishCollator) { it.name })
    }

    fun byCode(code: String?): Country? = code?.let { c -> all.firstOrNull { it.code == c } }
}
