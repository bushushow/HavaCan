package com.kampplus.havacan.data

import com.kampplus.havacan.model.City
import java.util.Locale

private val turkish = Locale.forLanguageTag("tr")

/**
 * Lowercases with Turkish rules and folds Turkish letters to ASCII,
 * so "sanliurfa", "ŞANLIURFA" and "Şanlıurfa" all match each other.
 */
fun normalizeForSearch(text: String): String = text.trim().lowercase(turkish).map { char ->
    when (char) {
        'ç' -> 'c'
        'ğ' -> 'g'
        'ı' -> 'i'
        'ö' -> 'o'
        'ş' -> 's'
        'ü' -> 'u'
        else -> char
    }
}.joinToString("")

/** Cities whose name contains [query]; all cities for a blank query. */
fun List<City>.filterByQuery(query: String): List<City> {
    val needle = normalizeForSearch(query)
    if (needle.isEmpty()) return this
    return filter { normalizeForSearch(it.name).contains(needle) }
}
