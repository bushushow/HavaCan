package com.kampplus.havacan.data.wardrobe

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kampplus.havacan.model.ClothingCategory
import com.kampplus.havacan.model.Season
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Room on a real device/emulator: items and outfits are stored and read back as written. */
@RunWith(AndroidJUnit4::class)
class WardrobeDaoTest {
    private lateinit var db: WardrobeDatabase
    private lateinit var dao: WardrobeDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, WardrobeDatabase::class.java).build()
        dao = db.wardrobe()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun storesAndReturnsItemsNewestFirst() = runBlocking {
        dao.upsert(ClothingEntity("a", "/p/a.jpg", "Krem kazak", ClothingCategory.Top, "Krem", setOf(Season.Autumn, Season.Winter), 1))
        dao.upsert(ClothingEntity("b", "/p/b.jpg", "Mavi jean", ClothingCategory.Bottom, "Kot mavisi", emptySet(), 2))

        val items = dao.observeItems().first()
        assertEquals(listOf("b", "a"), items.map { it.id })
        assertEquals(setOf(Season.Autumn, Season.Winter), items.last().seasons)
        assertEquals(ClothingCategory.Top, dao.item("a")!!.category)
    }

    @Test
    fun deleteAndOutfits() = runBlocking {
        val item = ClothingEntity("a", "/p/a.jpg", "Kazak", ClothingCategory.Top, "Krem", emptySet(), 1)
        dao.upsert(item)
        dao.upsert(SavedOutfitEntity("o", listOf("a", "b", "c"), "23° için rahat", 5))
        dao.delete(item)
        assertNull(dao.item("a"))
        assertEquals(listOf("a", "b", "c"), dao.observeOutfits().first().single().itemIds)
        dao.deleteOutfit("o")
        assertEquals(0, dao.observeOutfits().first().size)
    }
}
