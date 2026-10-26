package com.kampplus.havacan.domain.voice

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {
    private val today = LocalDate.parse("2026-10-26") // a Monday

    private fun parse(text: String) = VoiceCommandParser.parse(text, today)

    @Test
    fun demoScenarioCommands() {
        assertEquals(VoiceCommand.Ask(VoiceQuery.Weather(0)), parse("Hava durumunu söyle"))
        assertEquals(VoiceCommand.Navigate(VoiceDestination.Plans, VoiceQuery.Plans), parse("Planlarımı göster"))
        assertEquals(VoiceCommand.Navigate(VoiceDestination.Home), parse("Ana sayfaya dön"))
        assertEquals(VoiceCommand.EndSession, parse("Sesli rehberi kapat"))
    }

    @Test
    fun exampleCommands() {
        assertEquals(VoiceCommand.Ask(VoiceQuery.Weather(0)), parse("Bugün hava nasıl?"))
        assertEquals(VoiceCommand.Ask(VoiceQuery.Plans), parse("Planlarımı oku."))
        assertEquals(VoiceCommand.Navigate(VoiceDestination.Wardrobe), parse("Dolabıma git."))
        assertEquals(VoiceCommand.Ask(VoiceQuery.Outfit(1)), parse("Yarın ne giyeyim?"))
        assertEquals(VoiceCommand.Navigate(VoiceDestination.RunPlanner), parse("Koşu planla."))
        assertEquals(VoiceCommand.EndSession, parse("Sesli rehberi durdur."))
        assertEquals(VoiceCommand.Ask(VoiceQuery.RunTime(1)), parse("Yarın koşuya kaçta çıkayım?"))
    }

    @Test
    fun withoutTurkishLettersToo() {
        assertEquals(VoiceCommand.Navigate(VoiceDestination.Wardrobe), parse("dolabima git"))
        assertEquals(VoiceCommand.Ask(VoiceQuery.Outfit(1)), parse("yarin ne giyeyim"))
    }

    @Test
    fun yesNoAndHelp() {
        assertEquals(VoiceCommand.Yes, parse("Evet"))
        assertEquals(VoiceCommand.Yes, parse("Evet, onaylıyorum"))
        assertEquals(VoiceCommand.No, parse("Hayır"))
        assertEquals(VoiceCommand.No, parse("Vazgeç"))
        assertEquals(VoiceCommand.Help, parse("Neler yapabilirsin?"))
    }

    @Test
    fun readingPlansIsNotCreatingOne() {
        assertTrue(parse("Planlarımı kontrol et") is VoiceCommand.Ask)
        assertTrue(parse("Planlarım") is VoiceCommand.Navigate)
    }

    @Test
    fun createPlanTakesNameDateAndTime() {
        val command = parse("Yarın saat 6'da parkta piknik planı oluştur") as VoiceCommand.CreatePlan
        assertEquals(PlanDraft("Parkta piknik", today.plusDays(1), LocalTime.of(18, 0)), command.draft)
    }

    @Test
    fun createPlanWithMissingParts() {
        val command = parse("Yeni bir plan oluştur") as VoiceCommand.CreatePlan
        assertEquals(PlanDraft(), command.draft)
    }

    @Test
    fun deleteAndEditAreActions() {
        assertEquals(VoiceCommand.DeletePlan("piknik"), parse("Piknik planını sil"))
        assertEquals(VoiceCommand.EditPlan, parse("Piknik planını yarına ertele"))
    }

    @Test
    fun dates() {
        assertEquals(today.plusDays(1), VoiceCommandParser.date("yarın", today))
        assertEquals(LocalDate.parse("2026-10-31"), VoiceCommandParser.date("cumartesi", today))
        assertEquals(LocalDate.parse("2026-10-30"), VoiceCommandParser.date("cuma akşamı", today))
        assertEquals(LocalDate.parse("2026-11-03"), VoiceCommandParser.date("3 Kasım", today))
        assertEquals(LocalDate.parse("2027-01-05"), VoiceCommandParser.date("5 ocakta", today))
        assertNull(VoiceCommandParser.date("piknik", today))
    }

    @Test
    fun times() {
        assertEquals(LocalTime.of(18, 30), VoiceCommandParser.time("18:30"))
        assertEquals(LocalTime.of(19, 0), VoiceCommandParser.time("akşam 7"))
        assertEquals(LocalTime.of(9, 30), VoiceCommandParser.time("sabah 9 buçukta"))
        assertEquals(LocalTime.of(10, 0), VoiceCommandParser.time("saat 10'da"))
        assertEquals(LocalTime.of(18, 0), VoiceCommandParser.time("saat altıda"))
        assertEquals(LocalTime.NOON, VoiceCommandParser.time("öğlen"))
        assertNull(VoiceCommandParser.time("2 kişilik masa"))
        assertNull(VoiceCommandParser.time("3 Kasım"))
    }

    @Test
    fun turkishSuffixes() {
        assertEquals("Ankara'da", TurkishText.locative("Ankara"))
        assertEquals("Kadıköy'de", TurkishText.locative("Kadıköy"))
        assertEquals("Sinop'ta", TurkishText.locative("Sinop"))
        assertEquals("İzmit'te", TurkishText.locative("İzmit"))
    }

    @Test
    fun speechText() {
        assertEquals("16 derece, yüzde 40 yağış", TurkishText.forSpeech("16° · %40 yağış"))
    }
}
