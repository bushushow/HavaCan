package com.example.weather.data

import com.example.weather.model.Plan
import com.example.weather.model.PlanType
import com.example.weather.model.Reminder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlanStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val ankara = provinceCities.first { it.name == "Ankara" }

    @Test
    fun savesSortsAndReloadsFromDisk() = runTest {
        val file = tmp.newFile("plans.json").apply { delete() }
        val store = FilePlanStore(file)
        store.upsert(Plan("b", "Akşam yemeği", "2026-10-26", "19:00", ankara, type = PlanType.Indoor))
        store.upsert(Plan("a", "Sabah koşusu", "2026-10-26", "08:00", ankara, reminder = Reminder.OneHour))

        val reloaded = FilePlanStore(file).apply { load() }
        assertEquals(listOf("a", "b"), reloaded.plans.value.map { it.id })
        assertEquals(Reminder.OneHour, reloaded.get("a")!!.reminder)
        assertEquals("Ankara", reloaded.get("b")!!.city.name)
    }

    @Test
    fun upsertReplacesAndDeleteRemoves() = runTest {
        val store = FilePlanStore(tmp.newFolder().resolve("plans.json"))
        store.upsert(Plan("a", "Piknik", "2026-10-26", "12:00", ankara))
        store.upsert(Plan("a", "Piknik (güncel)", "2026-10-26", "13:00", ankara))
        assertEquals(listOf("Piknik (güncel)"), store.plans.value.map { it.title })
        store.delete("a")
        assertNull(store.get("a"))
    }

    @Test
    fun corruptFileStartsEmpty() = runTest {
        val file = tmp.newFile("plans.json").apply { writeText("{not json") }
        val store = FilePlanStore(file).apply { load() }
        assertEquals(emptyList<Plan>(), store.plans.value)
    }
}
