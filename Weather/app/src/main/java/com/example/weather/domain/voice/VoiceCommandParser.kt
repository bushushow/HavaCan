package com.example.weather.domain.voice

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Screens a voice command may open. The app shell decides how each one is shown. */
enum class VoiceDestination { Home, Plans, Chat, Wardrobe, Profile, RunPlanner }

/** Questions answered from the app's own data (forecast, plans, wardrobe). */
sealed interface VoiceQuery {
    data class Weather(val dayOffset: Int) : VoiceQuery
    data object Plans : VoiceQuery
    data class Outfit(val dayOffset: Int) : VoiceQuery
    data class RunTime(val dayOffset: Int) : VoiceQuery
}

/** What the user's sentence asks for, before any data is read. */
sealed interface VoiceCommand {
    /** Navigation intent; [then] is answered after the screen opens ("Planlarımı göster"). */
    data class Navigate(val to: VoiceDestination, val then: VoiceQuery? = null) : VoiceCommand

    /** Information query. */
    data class Ask(val query: VoiceQuery) : VoiceCommand

    /** Action requests: each one needs the user's explicit "evet" before anything is written. */
    data class CreatePlan(val draft: PlanDraft) : VoiceCommand
    data class DeletePlan(val name: String?) : VoiceCommand
    data object EditPlan : VoiceCommand

    /** Ends the voice session. */
    data object EndSession : VoiceCommand

    data object Help : VoiceCommand
    data object Yes : VoiceCommand
    data object No : VoiceCommand
    data class Unknown(val text: String) : VoiceCommand
}

/** A plan being put together by voice; null fields are still to be asked. */
data class PlanDraft(
    val title: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val type: com.example.weather.model.PlanType = com.example.weather.model.PlanType.Outdoor,
)

/**
 * Turns a Turkish sentence into a [VoiceCommand] with fixed rules, no AI: the same words always
 * do the same thing, which is what a voice-driven (and screen reader) user needs to rely on.
 */
object VoiceCommandParser {
    private val NAV_VERB = Regex("\\b(git|gidelim|gec|ac|acar misin|goster|don|donelim|gotur|getir)\\w*")
    private val END_VERB = Regex("\\b(kapat|durdur|bitir|sonlandir|sus)\\w*")
    // "planla" only as a whole word: "planlarımı" is not a request to create one
    private val CREATE_VERB = Regex("\\b(olustur\\w*|ekle\\w*|yap\\w*|kur|kurar\\w*|yarat\\w*|planla|planlayalim|planlar misin|ayarla\\w*|kaydet\\w*)\\b")
    private val EDIT_VERB = Regex("\\b(degistir|ertele|tasi|guncelle|kaydir|duzenle)\\w*")
    private val DELETE_VERB = Regex("\\b(sil|kaldir)\\w*|\\biptal et")
    private val READ_PLANS = Regex("\\b(oku|neler|ne var|kontrol|listele|soyle|hangi|var mi|yaklasan|anlat|ozetle)\\w*")
    private val YES = Regex("^(evet|onayla|onayliyorum|onay|tamam|olur|kaydet|tabii|tabi|aynen|dogru|kesinlikle)\\b.*")
    private val NO = Regex("^(hayir|iptal|vazgec|istemiyorum|gerek yok|olmasin|yok|kalsin)\\w*.*")
    private val WEATHER = Regex("\\b(hava|derece|sicaklik|yagmur|yagis|semsiye|kar yag|ruzgar|soguk|sicak|mont|ceket|firtina)\\w*")
    private val OUTFIT = Regex("\\b(giy|kombin|kiyafet|ne giy)\\w*")
    private val HELP = Regex("\\b(ne yapabilirsin|neler yapabil|yardim|komutlar|nasil kullan)\\w*")
    private val RUN = Regex("\\bkosu\\w*")
    private val RUN_TIME = Regex("\\b(kacta|ne zaman|saat kac|uygun|en iyi|cikayim|cikabilir|cikmali|kosayim|kosabilir)\\w*")

    private val MONTHS = listOf("ocak", "subat", "mart", "nisan", "mayis", "haziran", "temmuz", "agustos", "eylul", "ekim", "kasim", "aralik")
    private val DATE = Regex("\\b(\\d{1,2}) (${MONTHS.joinToString("|")})\\w*")
    private val CLOCK = Regex("\\b(\\d{1,2})[:.](\\d{2})\\b")
    private val HOUR = Regex("\\b(?:saat )?(\\d{1,2})(?: (bucuk))?(?: ?(?:de|da|te|ta|gibi|civari))?\\b")
    private val NUMBER_WORDS = listOf("bir", "iki", "uc", "dort", "bes", "alti", "yedi", "sekiz", "dokuz", "on", "on bir", "on iki")

    /** Words that are command, date or time, not part of a plan's name. */
    private val NOT_TITLE = setOf(
        "bir", "yeni", "icin", "saat", "de", "da", "te", "ta", "aksam", "sabah", "ogle", "oglen", "ogleden", "sonra",
        "gece", "lutfen", "bana", "benim", "adli", "isimli", "adinda", "bucuk", "gibi", "civari", "ve", "bugun", "yarin",
        "obur", "ertesi", "gun", "havacan", "hey", "olarak", "adi", "ismi", "olsun",
    )

    fun parse(text: String, today: LocalDate): VoiceCommand {
        val t = TurkishText.fold(text)
        if (t.isBlank()) return VoiceCommand.Unknown(text)
        val words = t.split(' ')

        if (END_VERB.containsMatchIn(t) && (words.size <= 2 || Regex("rehber|dinle|sesli|asistan|mikrofon|konusma").containsMatchIn(t))) {
            return VoiceCommand.EndSession
        }
        if (t in setOf("dur", "yeter", "gorusuruz", "hosca kal", "bu kadar", "tesekkurler bu kadar")) return VoiceCommand.EndSession
        if (words.size <= 3 && NO.matches(t)) return VoiceCommand.No
        if (words.size <= 3 && YES.matches(t)) return VoiceCommand.Yes
        if (HELP.containsMatchIn(t)) return VoiceCommand.Help

        val mentionsPlan = Regex("\\bplan\\w*").containsMatchIn(t)
        val day = dayOffset(t, today)

        if (RUN.containsMatchIn(t)) {
            if (RUN_TIME.containsMatchIn(t)) return VoiceCommand.Ask(VoiceQuery.RunTime(day ?: 0))
            if (mentionsPlan || NAV_VERB.containsMatchIn(t) || CREATE_VERB.containsMatchIn(t)) return VoiceCommand.Navigate(VoiceDestination.RunPlanner)
        }
        if (mentionsPlan && DELETE_VERB.containsMatchIn(t)) return VoiceCommand.DeletePlan(planName(text))
        if (mentionsPlan && EDIT_VERB.containsMatchIn(t)) return VoiceCommand.EditPlan
        if (Regex("\\btakvim\\w*").containsMatchIn(t) && (CREATE_VERB.containsMatchIn(t) || EDIT_VERB.containsMatchIn(t))) return VoiceCommand.EditPlan
        if (mentionsPlan && CREATE_VERB.containsMatchIn(t)) return VoiceCommand.CreatePlan(draft(text, today))

        destination(t)?.let { to ->
            val shortName = words.size == 1 // "planlarım", "dolabım"
            if (NAV_VERB.containsMatchIn(t) || shortName || to == VoiceDestination.Home) {
                val then = when {
                    to == VoiceDestination.Plans -> VoiceQuery.Plans
                    to == VoiceDestination.Home && WEATHER.containsMatchIn(t) -> VoiceQuery.Weather(day ?: 0)
                    else -> null
                }
                return VoiceCommand.Navigate(to, then)
            }
        }
        if (mentionsPlan && (READ_PLANS.containsMatchIn(t) || words.size <= 3)) return VoiceCommand.Ask(VoiceQuery.Plans)
        if (OUTFIT.containsMatchIn(t)) return VoiceCommand.Ask(VoiceQuery.Outfit(day ?: 0))
        if (WEATHER.containsMatchIn(t)) return VoiceCommand.Ask(VoiceQuery.Weather(day ?: 0))
        return VoiceCommand.Unknown(text)
    }

    private fun destination(t: String): VoiceDestination? = when {
        Regex("ana ?sayfa|ana ekran|ana menu|baslangic|hava (ekran|sayfa)|geri don|hava durumunu goster").containsMatchIn(t) -> VoiceDestination.Home
        Regex("\\bplan\\w*").containsMatchIn(t) -> VoiceDestination.Plans
        Regex("\\b(dolab|dolap|gardirop|kiyafetlerim)\\w*").containsMatchIn(t) -> VoiceDestination.Wardrobe
        Regex("\\b(profil|ayar)\\w*").containsMatchIn(t) -> VoiceDestination.Profile
        Regex("\\b(sohbet|asistan)\\w*").containsMatchIn(t) -> VoiceDestination.Chat
        else -> null
    }

    /** 0 = today, 1 = tomorrow…; null when the sentence names no day. */
    fun dayOffset(text: String, today: LocalDate): Int? = date(text, today)?.let { (it.toEpochDay() - today.toEpochDay()).toInt() }

    fun date(text: String, today: LocalDate): LocalDate? {
        val t = TurkishText.fold(text)
        DATE.find(t)?.let { m ->
            val day = m.groupValues[1].toInt()
            val month = MONTHS.indexOf(m.groupValues[2].let { w -> MONTHS.first { w.startsWith(it) } }) + 1
            val candidate = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return null
            return if (candidate.isBefore(today)) candidate.plusYears(1) else candidate
        }
        val words = t.split(' ')
        return when {
            Regex("\\b(obur gun|ertesi gun)").containsMatchIn(t) -> today.plusDays(2)
            words.any { it.startsWith("yarin") } -> today.plusDays(1)
            words.any { it.startsWith("bugun") } || Regex("\\bbu (aksam|sabah|gece|ogle)").containsMatchIn(t) -> today
            else -> weekday(words)?.let { dow -> today.plusDays(((dow.value - today.dayOfWeek.value + 7) % 7).toLong()) }
        }
    }

    private fun weekday(words: List<String>): DayOfWeek? = words.firstNotNullOfOrNull { w ->
        when {
            w.startsWith("pazartesi") -> DayOfWeek.MONDAY
            w.startsWith("pazar") -> DayOfWeek.SUNDAY
            w == "sali" || w.startsWith("saliya") || w.startsWith("salidan") || w.startsWith("saligunu") -> DayOfWeek.TUESDAY
            w.startsWith("carsamba") -> DayOfWeek.WEDNESDAY
            w.startsWith("persembe") -> DayOfWeek.THURSDAY
            w.startsWith("cumartesi") -> DayOfWeek.SATURDAY
            w.startsWith("cuma") -> DayOfWeek.FRIDAY
            else -> null
        }
    }

    /**
     * "18:00", "saat 6'da", "akşam 7", "9 buçukta", "öğlen". A bare 1–6 is taken as afternoon
     * (people say "6'da" for 18.00); the plan summary reads the time back before anything is saved.
     */
    fun time(text: String): LocalTime? {
        val t = TurkishText.fold(text).replace(DATE, " ")
        CLOCK.find(t)?.let { m ->
            val h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            if (h in 0..23 && min in 0..59) return LocalTime.of(adjust(h, t), min)
        }
        val words = digitsFromWords(t)
        HOUR.findAll(words).firstOrNull { m ->
            // a number must come with "saat" or a time suffix, so "2 kişilik" isn't a time
            val whole = m.value
            whole.startsWith("saat") || Regex("(de|da|te|ta|gibi|civari|bucuk)$").containsMatchIn(whole) ||
                Regex("(aksam|sabah|gece|ogleden sonra) ${m.groupValues[1]}").containsMatchIn(words)
        }?.let { m ->
            val h = m.groupValues[1].toInt()
            if (h !in 0..23) return null
            val min = if (m.groupValues[2].isNotEmpty()) 30 else 0
            return LocalTime.of(adjust(h, words), min)
        }
        if (Regex("\\b(oglen|ogle vakti|ogleyin)").containsMatchIn(t)) return LocalTime.NOON
        return null
    }

    private fun adjust(h: Int, t: String): Int = when {
        h >= 12 -> h
        Regex("\\b(aksam|gece|ogleden sonra)").containsMatchIn(t) -> h + 12
        Regex("\\bsabah").containsMatchIn(t) -> h
        h in 1..6 -> h + 12
        else -> h
    }

    /** "saat altıda" → "saat 6 da"; only after "saat", where a number word is surely a time. */
    private fun digitsFromWords(t: String): String {
        var out = t
        NUMBER_WORDS.withIndex().reversed().forEach { (i, w) ->
            out = out.replace(Regex("\\b(saat|aksam|sabah) $w(da|de|ta|te)?\\b")) { m -> "${m.groupValues[1]} ${i + 1}${if (m.groupValues[2].isNotEmpty()) " " + m.groupValues[2] else ""}" }
        }
        return out
    }

    /** Date, time and a name pulled out of one sentence ("Yarın saat 6'da parkta piknik planı oluştur"). */
    fun draft(text: String, today: LocalDate): PlanDraft =
        PlanDraft(title = title(text), date = date(text, today), time = time(text))

    /** What's left once command, date and time words are taken out, in the user's own spelling. */
    fun title(text: String): String? {
        val tokens = TurkishText.clean(text).split(' ').filter { it.isNotBlank() }
        val kept = tokens.filter { token ->
            val f = TurkishText.foldWord(token)
            !(f in NOT_TITLE || f.startsWith("plan") || CREATE_VERB.matches(f) || f.any(Char::isDigit) ||
                weekday(listOf(f)) != null || MONTHS.any { f.startsWith(it) } || f.startsWith("yarin") || f.startsWith("bugun") ||
                f in NUMBER_WORDS || f.endsWith("ki") && (f.startsWith("aksam") || f.startsWith("sabah")))
        }
        return kept.joinToString(" ").takeIf { it.length >= 2 }?.let(TurkishText::capitalize)
    }

    /** The plan's name in "Piknik planını sil", or null for "planımı sil". */
    private fun planName(text: String): String? {
        val tokens = TurkishText.clean(text).split(' ').filter { it.isNotBlank() }
        val kept = tokens.filterNot { token ->
            val f = TurkishText.foldWord(token)
            f.startsWith("plan") || DELETE_VERB.matches(f) || f in setOf("iptal", "et", "lutfen", "benim", "bu", "su", "olan")
        }
        return kept.joinToString(" ").takeIf { it.length >= 2 }
    }
}
