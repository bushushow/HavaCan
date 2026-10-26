package com.kampplus.havacan.domain.wardrobe

import com.kampplus.havacan.model.ClothingCategory
import com.kampplus.havacan.model.ClothingItem
import com.kampplus.havacan.model.Season
import com.kampplus.havacan.model.deg
import kotlin.math.roundToInt
import kotlin.random.Random

/** The weather an outfit is chosen for; null weather means it couldn't be loaded. */
data class WeatherContext(
    val placeName: String,
    val feelsLike: Double,
    val rainLikely: Boolean,
    val windKmh: Double
)

sealed interface OutfitResult {
    /** [items] are always pieces from the user's own wardrobe. */
    data class Ready(val items: List<ClothingItem>, val reason: String, val note: String?) : OutfitResult

    /** Not enough clothes: which categories to add. */
    data class Missing(val categories: List<ClothingCategory>) : OutfitResult
}

/**
 * Builds an outfit only from the given items, never inventing a piece. Rules are HavaCan's own
 * starting assumptions (feels below 15 °C or likely rain → outer layer), not fashion law.
 * Pure Kotlin; [random] makes "shake again" pick a different combination.
 */
object OutfitEngine {
    const val OUTER_LAYER_BELOW = 15.0
    private const val DRESS_CHANCE = 0.4
    private const val CANDIDATES = 40

    fun build(
        items: List<ClothingItem>,
        weather: WeatherContext?,
        month: Int,
        random: Random,
        avoid: Set<String> = emptySet()
    ): OutfitResult {
        val season = Season.ofMonth(month)
        val byCategory = items.groupBy { it.category }
        fun has(c: ClothingCategory) = !byCategory[c].isNullOrEmpty()

        val missing = buildList {
            if (!has(ClothingCategory.Dress) && !has(ClothingCategory.Top)) add(ClothingCategory.Top)
            if (!has(ClothingCategory.Dress) && !has(ClothingCategory.Bottom)) add(ClothingCategory.Bottom)
            if (!has(ClothingCategory.Shoes)) add(ClothingCategory.Shoes)
        }
        if (missing.isNotEmpty()) return OutfitResult.Missing(missing)

        var offSeason = false

        // Prefer pieces marked for this season (or for any season); fall back to the rest
        fun pool(c: ClothingCategory): List<ClothingItem> {
            val all = byCategory[c].orEmpty()
            val inSeason = all.filter { it.seasons.isEmpty() || season in it.seasons }
            return inSeason.ifEmpty { all.also { if (it.isNotEmpty()) offSeason = true } }
        }

        val needsOuter = weather?.let { it.feelsLike < OUTER_LAYER_BELOW || it.rainLikely } ?: (season == Season.Winter)
        val canTopBottom = has(ClothingCategory.Top) && has(ClothingCategory.Bottom)
        val useDress = has(ClothingCategory.Dress) && (!canTopBottom || random.nextDouble() < DRESS_CHANCE)

        val slots = buildList {
            if (useDress) {
                add(pool(ClothingCategory.Dress))
            } else {
                add(pool(ClothingCategory.Top))
                add(pool(ClothingCategory.Bottom))
            }
            if (needsOuter && has(ClothingCategory.Outerwear)) add(pool(ClothingCategory.Outerwear))
            add(pool(ClothingCategory.Shoes))
        }

        val best = (0 until CANDIDATES)
            .map { slots.map { pool -> pickAvoiding(pool, avoid, random) } }
            .distinct()
            .maxBy { harmony(it) + random.nextDouble() } // a little randomness between equally good outfits

        val note = when {
            needsOuter && !has(ClothingCategory.Outerwear) -> "Hava serin; dolabına dış giyim eklersen kombine katarım."
            offSeason -> "Bu mevsime işaretli parça yetmedi; diğer mevsimlerden tamamladım."
            else -> null
        }
        return OutfitResult.Ready(best, reason(weather, season, best), note)
    }

    private fun pickAvoiding(pool: List<ClothingItem>, avoid: Set<String>, random: Random): ClothingItem {
        val fresh = pool.filter { it.id !in avoid }
        return (fresh.ifEmpty { pool }).random(random)
    }

    /** Neutrals go with anything; two different loud colours clash. */
    internal fun harmony(outfit: List<ClothingItem>): Double {
        val colors = outfit.map { it.colorName }
        val loud = colors.filterNot { ClothingColors.isNeutral(it) }.distinct()
        var score = colors.count { ClothingColors.isNeutral(it) } * 1.0
        if (loud.size >= 2) score -= 3.0 * (loud.size - 1)
        if (loud.size == 1) score += 1.0 // one accent colour
        return score
    }

    internal fun reason(weather: WeatherContext?, season: Season, outfit: List<ClothingItem>): String {
        if (weather == null) return "Hava bilgisi alınamadı; ${season.label.lowercase()} mevsimine göre seçtim."
        val feels = weather.feelsLike.roundToInt()
        val shown = weather.feelsLike.deg()
        val outer = outfit.firstOrNull { it.category == ClothingCategory.Outerwear }
        val wind = when {
            weather.windKmh >= 30 -> "rüzgarlı hava"
            weather.windKmh >= 12 -> "hafif esinti"
            else -> "sakin hava"
        }
        return when {
            weather.rainLikely && outer != null -> "$shown° ve yağmur ihtimali için ${outer.name.lowercase()} ekledim."
            weather.rainLikely -> "$shown° ve yağmur ihtimali var; şemsiyeni unutma."
            outer != null -> "$shown° ve $wind için ${outer.name.lowercase()} ile katmanlı bir tercih."
            feels >= 26 -> "$shown° için hafif ve serin bir tercih."
            else -> "$shown° ve $wind için rahat bir tercih."
        }
    }
}
