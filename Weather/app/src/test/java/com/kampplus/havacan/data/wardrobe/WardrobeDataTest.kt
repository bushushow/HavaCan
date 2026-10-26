package com.kampplus.havacan.data.wardrobe

import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WardrobeDataTest {

    @Test
    fun aiAnswerIsCheckedAgainstOurLists() {
        val guess = BackendClothingAnalyzer.parseGuess(
            """{"name":"Krem kazak","category":"Top","color":"Krem","seasons":["Autumn","Winter","Monsoon"]}"""
        )
        assertEquals("Krem kazak", guess.name)
        assertEquals(ClothingCategory.Top, guess.category)
        assertEquals("Krem", guess.colorName)
        assertEquals(setOf(Season.Autumn, Season.Winter), guess.seasons)
    }

    @Test
    fun unknownValuesAreDroppedNotGuessed() {
        val guess = BackendClothingAnalyzer.parseGuess("""{"name":"  ","category":"Hat","color":"Fuchsia","seasons":null}""")
        assertNull(guess.name)
        assertNull(guess.category)
        assertNull(guess.colorName)
        assertTrue(guess.seasons.isEmpty())
    }

    @Test(expected = IOException::class)
    fun garbageIsAnError() {
        BackendClothingAnalyzer.parseGuess("not json at all {")
    }

    @Test
    fun convertersRoundTrip() {
        val c = WardrobeConverters()
        val seasons = setOf(Season.Spring, Season.Winter)
        assertEquals(seasons, c.stringToSeasons(c.seasonsToString(seasons)))
        assertEquals(emptySet<Season>(), c.stringToSeasons(""))
        val ids = listOf("a", "b", "c")
        assertEquals(ids, c.stringToIds(c.idsToString(ids)))
    }

    @Test
    fun seasonOfMonth() {
        assertEquals(Season.Autumn, Season.ofMonth(10))
        assertEquals(Season.Winter, Season.ofMonth(1))
        assertEquals(Season.Summer, Season.ofMonth(7))
    }

    @Test
    fun onlyOldUnreferencedPhotosAreDeleted() {
        val dir = Files.createTempDirectory("wardrobe").toFile()
        val now = 1_000_000_000L
        fun photo(name: String, ageMs: Long) = java.io.File(dir, name).apply {
            writeText("x")
            setLastModified(now - ageMs)
        }
        val saved = photo("saved.jpg", 3_600_000)
        val orphan = photo("orphan.jpg", 3_600_000)
        val importing = photo("new.jpg", 5_000)

        assertEquals(1, PhotoStore.deleteOrphans(dir, setOf(saved.absolutePath), now))
        assertTrue(saved.exists())
        assertTrue(!orphan.exists())
        assertTrue(importing.exists())
        dir.deleteRecursively()
    }
}
