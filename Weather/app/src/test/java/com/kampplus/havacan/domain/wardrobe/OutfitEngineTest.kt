package com.kampplus.havacan.domain.wardrobe

import com.kampplus.havacan.model.ClothingCategory
import com.kampplus.havacan.model.ClothingCategory.Bottom
import com.kampplus.havacan.model.ClothingCategory.Dress
import com.kampplus.havacan.model.ClothingCategory.Outerwear
import com.kampplus.havacan.model.ClothingCategory.Shoes
import com.kampplus.havacan.model.ClothingCategory.Top
import com.kampplus.havacan.model.ClothingItem
import com.kampplus.havacan.model.Season
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutfitEngineTest {
    private fun item(id: String, category: ClothingCategory, color: String = "Beyaz", seasons: Set<Season> = emptySet()) =
        ClothingItem(id, "/x/$id.jpg", "$color ${category.label}", category, color, seasons, 0)

    private val mild = WeatherContext("Ankara", 23.0, rainLikely = false, windKmh = 14.0)
    private val coldRain = WeatherContext("Ankara", 9.0, rainLikely = true, windKmh = 20.0)
    private val october = 10

    @Test
    fun emptyWardrobeAsksForTopBottomAndShoes() {
        val result = OutfitEngine.build(emptyList(), mild, october, Random(1))
        assertEquals(listOf(Top, Bottom, Shoes), (result as OutfitResult.Missing).categories)
    }

    @Test
    fun missingShoesIsReported() {
        val result = OutfitEngine.build(listOf(item("t", Top), item("b", Bottom)), mild, october, Random(1))
        assertEquals(listOf(Shoes), (result as OutfitResult.Missing).categories)
    }

    @Test
    fun dressPlusShoesIsEnough() {
        val result = OutfitEngine.build(listOf(item("d", Dress), item("s", Shoes)), mild, october, Random(1))
        assertEquals(listOf("d", "s"), (result as OutfitResult.Ready).items.map { it.id })
    }

    @Test
    fun onlyRealItemsAreUsedAndReasonUsesWeather() {
        val wardrobe = listOf(item("t", Top, "Krem"), item("b", Bottom, "Kot mavisi"), item("s", Shoes))
        val result = OutfitEngine.build(wardrobe, mild, october, Random(7)) as OutfitResult.Ready
        assertTrue(wardrobe.containsAll(result.items))
        assertEquals("23° ve hafif esinti için rahat bir tercih.", result.reason)
        assertNull(result.note)
    }

    @Test
    fun coldOrRainAddsOuterwearWhenAvailable() {
        val wardrobe = listOf(item("t", Top), item("b", Bottom), item("s", Shoes), item("o", Outerwear, "Lacivert"))
        val result = OutfitEngine.build(wardrobe, coldRain, october, Random(3)) as OutfitResult.Ready
        assertTrue(result.items.any { it.category == Outerwear })
        assertTrue(result.reason.startsWith("9° ve yağmur ihtimali için"))
    }

    @Test
    fun coldWithoutOuterwearStillDressesButSaysSo() {
        val wardrobe = listOf(item("t", Top), item("b", Bottom), item("s", Shoes))
        val result = OutfitEngine.build(wardrobe, coldRain, october, Random(3)) as OutfitResult.Ready
        assertEquals(3, result.items.size)
        assertTrue(result.note!!.contains("dış giyim"))
    }

    @Test
    fun prefersInSeasonPieces() {
        val wardrobe = listOf(
            item("summerTop", Top, seasons = setOf(Season.Summer)),
            item("autumnTop", Top, seasons = setOf(Season.Autumn, Season.Winter)),
            item("b", Bottom),
            item("s", Shoes)
        )
        repeat(10) { seed ->
            val result = OutfitEngine.build(wardrobe, mild, october, Random(seed)) as OutfitResult.Ready
            assertTrue(result.items.any { it.id == "autumnTop" })
        }
    }

    @Test
    fun shakeAgainAvoidsThePreviousPiecesWhenPossible() {
        val wardrobe = listOf(item("t1", Top), item("t2", Top), item("b", Bottom), item("s", Shoes))
        val first = OutfitEngine.build(wardrobe, mild, october, Random(1)) as OutfitResult.Ready
        val second = OutfitEngine.build(wardrobe, mild, october, Random(2), avoid = first.items.map { it.id }.toSet()) as OutfitResult.Ready
        assertNotEquals(first.items.first { it.category == Top }.id, second.items.first { it.category == Top }.id)
    }

    @Test
    fun noWeatherFallsBackToSeason() {
        val wardrobe = listOf(item("t", Top), item("b", Bottom), item("s", Shoes))
        val result = OutfitEngine.build(wardrobe, null, october, Random(1)) as OutfitResult.Ready
        assertEquals("Hava bilgisi alınamadı; sonbahar mevsimine göre seçtim.", result.reason)
    }

    @Test
    fun loudColoursClash() {
        assertTrue(
            OutfitEngine.harmony(listOf(item("a", Top, "Kırmızı"), item("b", Bottom, "Yeşil"))) <
                OutfitEngine.harmony(listOf(item("a", Top, "Kırmızı"), item("b", Bottom, "Lacivert")))
        )
    }

    @Test
    fun detectsGarmentColourAgainstBackground() {
        val w = 60
        val h = 60
        val grey = 0xE6E6E6
        val cream = 0xEDE0C4
        // Light grey backdrop with a cream sweater block in the middle
        val pixels = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            if (x in 12..47 && y in 10..50) cream else grey
        }
        val rgb = ColorDetector.dominantColor(pixels, w, h)!!
        assertEquals("Krem", ClothingColors.nearest(rgb).name)
    }

    @Test
    fun garmentTouchingTheEdgeOrFillingTheFrame() {
        val w = 60
        val h = 60
        val grey = 0xE6E6E6
        val cream = 0xEDE0C4
        // Sweater cut off by the bottom edge, as in a typical phone photo
        val cut = IntArray(w * h) { i -> if (i % w in 10..49 && i / w >= 8) cream else grey }
        assertEquals("Krem", ClothingColors.nearest(ColorDetector.dominantColor(cut, w, h)!!).name)
        // Close-up where the fabric fills the frame
        val navy = 0x1F2A4A
        assertEquals("Lacivert", ClothingColors.nearest(ColorDetector.dominantColor(IntArray(w * h) { navy }, w, h)!!).name)
        assertNull(ColorDetector.dominantColor(IntArray(4) { navy }, 2, 2))
    }
}
