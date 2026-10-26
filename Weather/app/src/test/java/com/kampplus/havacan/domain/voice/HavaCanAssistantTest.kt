package com.kampplus.havacan.domain.voice

import com.kampplus.havacan.data.provinceCities
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.ClothingCategory
import com.kampplus.havacan.model.ClothingItem
import com.kampplus.havacan.model.CurrentWeather
import com.kampplus.havacan.model.DailySummary
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.WeatherCondition
import java.io.IOException
import java.time.LocalDateTime
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HavaCanAssistantTest {
    private val now = LocalDateTime.parse("2026-10-26T10:00")
    private val ankara = provinceCities.first { it.name == "Ankara" }

    /** Clear and 16° all along, except rain tomorrow evening. */
    private fun hour(date: String, h: Int, rainy: Boolean = date == "2026-10-27" && h in 17..20) = HourlyWeather(
        time = "%sT%02d:00".format(date, h),
        temperature = 16.0,
        apparentTemperature = 15.0,
        precipitationProbability = if (rainy) 70 else 5,
        precipitation = if (rainy) 1.5 else 0.0,
        condition = if (rainy) WeatherCondition.Rain else WeatherCondition.Clear,
        isDay = h in 7..17,
        windSpeed = 6.0,
        windGusts = 12.0,
        uvIndex = 2.0
    )

    private val forecast = Forecast(
        timezone = "Europe/Istanbul",
        current = CurrentWeather("2026-10-26T10:00", 16.0, 15.0, WeatherCondition.Clear, true, 6.0),
        hourly = listOf("2026-10-26", "2026-10-27").flatMap { d -> (0..23).map { hour(d, it) } },
        today = DailySummary("2026-10-26", 19.0, 9.0, null, null, WeatherCondition.Clear, 5),
        daily = listOf(
            DailySummary("2026-10-26", 19.0, 9.0, null, null, WeatherCondition.Clear, 5),
            DailySummary("2026-10-27", 17.0, 8.0, null, null, WeatherCondition.Rain, 70)
        )
    )

    private class FakeData(var forecast: Forecast?, var offline: Boolean = false, val city: City) : AssistantData {
        val saved = mutableListOf<Plan>()
        val deleted = mutableListOf<Plan>()
        var stored = listOf<Plan>()
        var clothes = listOf<ClothingItem>()
        override fun homeCity() = city
        override suspend fun forecast(city: City): Forecast? = if (offline) throw IOException("offline") else forecast
        override suspend fun plans() = stored + saved
        override suspend fun savePlan(plan: Plan) {
            saved += plan
        }
        override suspend fun deletePlan(plan: Plan) {
            deleted += plan
        }
        override suspend fun wardrobe() = clothes

        /** Plays the AI backend; null means "not configured". */
        var ai: ((String, String) -> AiDecision)? = null
        override suspend fun askAi(message: String, context: String, history: List<AiTurn>): AiDecision? = ai?.invoke(message, context)
    }

    private val data = FakeData(forecast, city = ankara)
    private val assistant = HavaCanAssistant(data, now = { now }, newId = { "id-1" }, random = Random(1))

    @Test
    fun weatherComesFromTheForecast() = runTest {
        val reply = assistant.handle("Hava durumunu söyle")
        assertEquals("Ankara'da şu an 16°, açık.", reply.title)
        assertTrue(reply.body!!.contains("en yüksek 19°"))
        assertTrue(reply.spoken.contains("16 derece"))
    }

    @Test
    fun tomorrowWarnsAboutRain() = runTest {
        val reply = assistant.handle("Yarın hava nasıl?")
        assertTrue(reply.title.startsWith("Yarın Ankara'da"))
        assertTrue(reply.body!!.contains("Şemsiyeni unutma"))
    }

    @Test
    fun offlineNeverMakesUpNumbers() = runTest {
        data.offline = true
        val reply = assistant.handle("Bugün hava nasıl?")
        assertEquals("Hava bilgisini şu an alamadım.", reply.title)
        assertFalse(reply.spoken.any(Char::isDigit))
    }

    @Test
    fun showPlansNavigatesAndSummarises() = runTest {
        data.stored = listOf(Plan("p1", "Piknik", "2026-10-27", "18:00", ankara))
        val reply = assistant.handle("Planlarımı göster")
        assertEquals(VoiceDestination.Plans, reply.navigate)
        assertTrue(reply.body!!.contains("1 yaklaşan planın var"))
        assertTrue(reply.body!!.contains("Yarın 18:00, Piknik: yağmur riski"))
    }

    @Test
    fun noPlans() = runTest {
        assertEquals("Yaklaşan planın yok.", assistant.handle("Planlarımı oku").title)
    }

    @Test
    fun planIsSavedOnlyAfterYes() = runTest {
        assertEquals("Planın adı ne olsun?", assistant.handle("Plan oluştur").title)
        assertEquals("Hangi gün?", assistant.handle("Piknik").title)
        assertEquals("Saat kaçta?", assistant.handle("Yarın").title)
        val summary = assistant.handle("Akşam 6")
        assertTrue(summary.title.contains("Yarın saat 18:00"))
        assertTrue(summary.body!!.contains("Onaylıyor musun"))
        assertTrue(data.saved.isEmpty())

        val done = assistant.handle("Evet")
        assertEquals(1, data.saved.size)
        assertEquals("Piknik", data.saved.single().title)
        assertEquals("2026-10-27", data.saved.single().date)
        assertEquals("18:00", data.saved.single().time)
        assertTrue(done.title.contains("kaydettim"))
        assertFalse(assistant.isInDialog)
    }

    @Test
    fun noCancelsThePlan() = runTest {
        assistant.handle("Yarın saat 10'da piknik planı oluştur")
        assertEquals("Tamam, planı kaydetmedim.", assistant.handle("Hayır").title)
        assertTrue(data.saved.isEmpty())
    }

    @Test
    fun anythingButYesKeepsWaiting() = runTest {
        assistant.handle("Yarın saat 10'da piknik planı oluştur")
        assistant.handle("Belki")
        assertTrue(data.saved.isEmpty())
        assertTrue(assistant.isInDialog)
    }

    @Test
    fun pastTimeIsAskedAgain() = runTest {
        val reply = assistant.handle("Bugün saat 8'de kahvaltı planı oluştur")
        assertEquals("Bu saat geçmiş.", reply.title)
    }

    @Test
    fun deleteNeedsConfirmation() = runTest {
        data.stored = listOf(Plan("p1", "Piknik", "2026-10-27", "18:00", ankara))
        val ask = assistant.handle("Piknik planını sil")
        assertTrue(ask.title.contains("sileceğim"))
        assertTrue(data.deleted.isEmpty())
        assistant.handle("Evet")
        assertEquals("p1", data.deleted.single().id)
    }

    @Test
    fun endSession() = runTest {
        assistant.handle("Plan oluştur")
        val reply = assistant.handle("Sesli rehberi kapat")
        assertTrue(reply.endSession)
        assertFalse(assistant.isInDialog)
    }

    @Test
    fun runTimeUsesTheRunPlanner() = runTest {
        val reply = assistant.handle("Yarın koşuya kaçta çıkayım?")
        assertTrue(reply.title, reply.title.matches(Regex("Yarın \\d\\d:00 en uygun saat!")))
        assertEquals(VoiceDestination.RunPlanner, reply.card!!.opens)
    }

    @Test
    fun outfitNeedsClothes() = runTest {
        assertEquals("Kombin için dolabında yeterli parça yok.", assistant.handle("Yarın ne giyeyim?").title)
        data.clothes = listOf(
            ClothingItem("1", "", "Beyaz tişört", ClothingCategory.Top, "Beyaz", emptySet(), 0),
            ClothingItem("2", "", "Kot pantolon", ClothingCategory.Bottom, "Kot mavisi", emptySet(), 0),
            ClothingItem("3", "", "Spor ayakkabı", ClothingCategory.Shoes, "Beyaz", emptySet(), 0)
        )
        val reply = assistant.handle("Yarın ne giyeyim?")
        assertEquals("Yarın için kombinin hazır.", reply.title)
        assertTrue(reply.body!!.contains("Beyaz tişört"))
    }

    @Test
    fun unknownWithoutAiIsHonest() = runTest {
        val reply = assistant.handle("Bana bir fıkra anlat")
        assertEquals("Bunu anlayamadım.", reply.title)
        assertNull(reply.navigate)
    }

    @Test
    fun aiChangeIsReadBackAndSavedOnlyAfterYes() = runTest {
        data.stored = listOf(Plan("p1", "Piknik", "2026-10-27", "18:00", ankara))
        var seenContext = ""
        data.ai = { _, context ->
            seenContext = context
            AiDecision(null, AiAction("update_plan", planId = "p1", time = "19:00", indoor = true))
        }
        val ask = assistant.handle("Pikniği bir saat ileri al, içeride olsun")
        assertTrue(seenContext.contains("id=p1"))
        assertTrue(ask.title, ask.title.contains("yarın saat 19:00") && ask.title.contains("kapalı alan"))
        assertTrue(data.saved.isEmpty())
        assistant.handle("Evet")
        assertEquals("19:00", data.saved.single().time)
        assertEquals(com.kampplus.havacan.model.PlanType.Indoor, data.saved.single().type)
    }

    @Test
    fun aiCreateGoesThroughTheSameConfirmation() = runTest {
        data.ai = { _, _ -> AiDecision(null, AiAction("create_plan", title = "Ayşe ile kahve", date = "2026-10-29", time = "17:30")) }
        val ask = assistant.handle("Perşembe akşamüstü Ayşe'yle kahve içeceğim, not al")
        assertTrue(ask.body!!.contains("Onaylıyor musun"))
        assertTrue(data.saved.isEmpty())
        assistant.handle("Evet")
        assertEquals("Ayşe ile kahve", data.saved.single().title)
        assertEquals("2026-10-29", data.saved.single().date)
    }

    @Test
    fun aiDatesInThePastAreAskedAgain() = runTest {
        data.ai = { _, _ -> AiDecision(null, AiAction("create_plan", title = "Kahve", date = "2020-01-01", time = "17:30")) }
        assertEquals("Hangi gün?", assistant.handle("dün kahve içtim, plan yap").title)
    }

    @Test
    fun aiCanOnlyOpenKnownScreens() = runTest {
        data.ai = { _, _ -> AiDecision(null, AiAction("open_screen", screen = "Wardrobe")) }
        assertEquals(VoiceDestination.Wardrobe, assistant.handle("Şu sayfayı açar mısın").navigate)
        data.ai = { _, _ -> AiDecision(null, AiAction("open_screen", screen = "Settings.Secret")) }
        assertNull(assistant.handle("gizli ekranı aç").navigate)
    }

    @Test
    fun aiTextAnswerAndOutage() = runTest {
        data.ai = { _, _ -> AiDecision("Elbette, iyi eğlenceler!", null) }
        assertEquals("Elbette, iyi eğlenceler!", assistant.handle("Bana bir fıkra anlat").title)
        data.ai = { _, _ -> throw IOException("offline") }
        assertEquals("Yapay zekâ bağlantısına şu an ulaşamadım.", assistant.handle("Bana bir fıkra anlat").title)
    }

    @Test
    fun withoutAiChangingAPlanIsHonest() = runTest {
        assertEquals("Planları sesle değiştirmek için yapay zekâ bağlantısı gerekiyor.", assistant.handle("Piknik planını ertele").title)
    }

    @Test
    fun backendJsonIsParsed() {
        val d = com.kampplus.havacan.data.BackendAssistant.parse(
            """{"reply":null,"action":{"name":"update_plan","args":{"plan_id":"p1","time":"19:00","indoor":true}}}"""
        )
        assertEquals(AiAction("update_plan", planId = "p1", time = "19:00", indoor = true), d.action)
    }
}
