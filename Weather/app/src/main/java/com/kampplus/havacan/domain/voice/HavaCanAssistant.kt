package com.kampplus.havacan.domain.voice

import com.kampplus.havacan.domain.insight.WeatherInsight
import com.kampplus.havacan.domain.insight.WeatherInsightBuilder
import com.kampplus.havacan.domain.plans.PlanAdvisor
import com.kampplus.havacan.domain.plans.PlanOutlook
import com.kampplus.havacan.domain.running.RunDayOutlook
import com.kampplus.havacan.domain.running.RunSlotPlanner
import com.kampplus.havacan.domain.wardrobe.OutfitEngine
import com.kampplus.havacan.domain.wardrobe.OutfitResult
import com.kampplus.havacan.domain.wardrobe.WeatherContext
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.ClothingCategory
import com.kampplus.havacan.model.ClothingItem
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.PlanSource
import com.kampplus.havacan.model.PlanType
import com.kampplus.havacan.model.Reminder
import com.kampplus.havacan.model.TemperatureUnit
import com.kampplus.havacan.model.deg
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.random.Random

/** Everything the assistant may read or change. The app wires it to its real repositories. */
interface AssistantData {
    fun homeCity(): City

    /** Throws on network errors; null when the API had no data. */
    suspend fun forecast(city: City): Forecast?
    suspend fun plans(): List<Plan>
    suspend fun savePlan(plan: Plan)
    suspend fun deletePlan(plan: Plan)
    suspend fun wardrobe(): List<ClothingItem>

    /** The unit the user reads temperatures in (Profil). */
    fun temperatureUnit(): TemperatureUnit = TemperatureUnit.Celsius

    /**
     * A sentence the rules didn't understand, for the AI backend. It answers with text or picks one
     * of a fixed list of tools ([AiAction]); the assistant validates it and runs it through the same
     * confirmed flows as voice commands. Null when no backend is configured; throws when unreachable.
     */
    suspend fun askAi(message: String, context: String, history: List<AiTurn>): AiDecision?
}

data class AiTurn(val fromUser: Boolean, val text: String)

/** One tool call from the model; every field is checked again before use. */
data class AiAction(
    val name: String,
    val screen: String? = null,
    val dayOffset: Int = 0,
    val planId: String? = null,
    val title: String? = null,
    val date: String? = null,
    val time: String? = null,
    val indoor: Boolean? = null
)

data class AiDecision(val reply: String?, val action: AiAction?)

enum class CardKind { Weather, Run, Plans, Outfit, Plan }

/** The small tappable card under a reply in the chat. */
data class AssistantCard(val kind: CardKind, val title: String, val detail: String, val opens: VoiceDestination?)

data class AssistantReply(
    val title: String,
    val body: String? = null,
    val card: AssistantCard? = null,
    val navigate: VoiceDestination? = null,
    val endSession: Boolean = false
) {
    /** What HavaCan says out loud. */
    val spoken: String get() = TurkishText.forSpeech(listOfNotNull(title, body).joinToString(" "))
}

/**
 * HavaCan's answers for both the chat and the voice guide. Commands are parsed by fixed rules
 * ([VoiceCommandParser]); every number comes from the forecast, plans or wardrobe in [data].
 * Anything that writes (creating or deleting a plan) is read back and waits for an explicit
 * "evet". The AI backend only answers questions the rules don't cover, and only with text.
 */
class HavaCanAssistant(
    private val data: AssistantData,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
    private val random: Random = Random.Default
) {
    /** Temperatures in the user's unit. */
    private fun Double.deg(): Int = deg(data.temperatureUnit())

    private sealed interface Pending {
        data class Create(val draft: PlanDraft, val asking: Slot?) : Pending
        data class Delete(val plan: Plan) : Pending
        data class Update(val before: Plan, val after: Plan) : Pending
    }

    /** The last few exchanges, so the AI understands "peki yarın?". */
    private val history = ArrayDeque<AiTurn>()

    private enum class Slot { Title, Date, Time }

    private var pending: Pending? = null
    private val runPlanner = RunSlotPlanner()

    /** True while HavaCan waits for a plan detail or a yes/no. */
    val isInDialog: Boolean get() = pending != null

    suspend fun handle(text: String): AssistantReply {
        val reply = respond(text)
        remember(AiTurn(fromUser = true, text = text))
        remember(AiTurn(fromUser = false, text = listOfNotNull(reply.title, reply.body).joinToString(" ")))
        return reply
    }

    private fun remember(turn: AiTurn) {
        history.addLast(turn)
        while (history.size > HISTORY) history.removeFirst()
    }

    private suspend fun respond(text: String): AssistantReply {
        val today = now().toLocalDate()
        val command = VoiceCommandParser.parse(text, today)
        if (command == VoiceCommand.EndSession) {
            pending = null
            return AssistantReply("Sesli rehberi kapatıyorum.", "İstediğinde mikrofona dokunman yeterli.", endSession = true)
        }
        // Asked for a day or time but got a new request instead: drop the half-made plan, follow the request
        val p0 = pending
        if (p0 is Pending.Create && p0.asking != null && p0.asking != Slot.Title &&
            (command is VoiceCommand.Navigate || command is VoiceCommand.Ask || command == VoiceCommand.Help)
        ) {
            pending = null
        }
        when (val p = pending) {
            is Pending.Create -> return continueCreate(p, command, text, today)
            is Pending.Delete -> return continueDelete(p, command)
            is Pending.Update -> return continueUpdate(p, command)
            null -> Unit
        }
        return when (command) {
            is VoiceCommand.Navigate -> navigate(command)
            is VoiceCommand.Ask -> answer(command.query)
            is VoiceCommand.CreatePlan -> startCreate(command.draft, today)
            is VoiceCommand.DeletePlan -> startDelete(command.name)
            // Changing a plan needs the AI to read "pikniği bir saat ileri al"; without it, say so
            VoiceCommand.EditPlan -> unknown(text) ?: AssistantReply(
                "Planları sesle değiştirmek için yapay zekâ bağlantısı gerekiyor.",
                "Planlar ekranında plana dokunarak düzenleyebilirsin.",
                card = AssistantCard(CardKind.Plans, "Planlarım", "Planı ekrandan düzenle", VoiceDestination.Plans)
            )
            VoiceCommand.Help -> help()
            VoiceCommand.Yes, VoiceCommand.No -> AssistantReply("Şu an onay bekleyen bir işlem yok.")
            is VoiceCommand.Unknown -> unknown(text) ?: AssistantReply(
                "Bunu anlayamadım.",
                "Hava, planlar, kombin ve koşu hakkında sorabilirsin. Örneğin \"Yarın ne giyeyim?\""
            )
            VoiceCommand.EndSession -> error("handled above")
        }
    }

    // region Navigation and questions

    private suspend fun navigate(command: VoiceCommand.Navigate): AssistantReply {
        val opened = when (command.to) {
            VoiceDestination.Home -> "Ana sayfaya döndüm."
            VoiceDestination.Plans -> "Planlarım açıldı."
            VoiceDestination.Chat -> "Sohbet ekranı açıldı."
            VoiceDestination.Wardrobe -> "Dolabım açıldı."
            VoiceDestination.Profile -> "Profil açıldı."
            VoiceDestination.RunPlanner -> "Koşu Planla açıldı."
        }
        val then = command.then?.let { answer(it) }
        val body = when {
            then != null -> listOfNotNull(then.title, then.body).joinToString(" ")
            command.to == VoiceDestination.RunPlanner -> "Önerilen saatler ekranda, en uygunu en üstte."
            else -> null
        }
        return AssistantReply(opened, body, card = then?.card?.copy(opens = null), navigate = command.to)
    }

    private suspend fun answer(query: VoiceQuery): AssistantReply = when (query) {
        is VoiceQuery.Weather -> weather(query.dayOffset)
        VoiceQuery.Plans -> plans()
        is VoiceQuery.Outfit -> outfit(query.dayOffset)
        is VoiceQuery.RunTime -> runTime(query.dayOffset)
    }

    private suspend fun loadForecast(city: City): Result<Forecast?> = runCatching { data.forecast(city) }

    private fun noWeather() = AssistantReply(
        "Hava bilgisini şu an alamadım.",
        "İnternet bağlantını kontrol edip biraz sonra tekrar sor."
    )

    private suspend fun weather(dayOffset: Int): AssistantReply {
        val city = data.homeCity()
        val forecast = loadForecast(city).getOrNull() ?: return noWeather()
        val today = now().toLocalDate()
        val date = today.plusDays(dayOffset.toLong())
        val card = { detail: String -> AssistantCard(CardKind.Weather, city.name, detail, VoiceDestination.Home) }

        if (dayOffset == 0) {
            val c = forecast.current
            val todaySummary = forecast.daily.firstOrNull { it.date == date.toString() }
            val rain = when (val insight = WeatherInsightBuilder.build(forecast.upcoming)) {
                is WeatherInsight.RainNow -> "Şu an yağış var, şemsiyeni al."
                is WeatherInsight.RainComing -> "Saat ${insight.hour.hourLabel.replace('.', ':')} civarı yağmur bekleniyor" +
                    (insight.hour.precipitationProbability?.let { ", ihtimal %$it." } ?: ".")
                is WeatherInsight.Dry -> "Önümüzdeki saatlerde yağmur beklenmiyor."
                null -> null
            }
            val range = todaySummary?.let { "Bugün en yüksek ${it.temperatureMax.deg()}°, en düşük ${it.temperatureMin.deg()}°." }
            return AssistantReply(
                title = "${TurkishText.locative(city.name)} şu an ${c.temperature.deg()}°, ${c.condition.label.lowercase(TurkishText.TR)}.",
                body = listOfNotNull("Hissedilen ${c.apparentTemperature.deg()}°.", range, rain).joinToString(" "),
                card = card("${c.temperature.deg()}° · ${c.condition.label}")
            )
        }
        val day = forecast.daily.firstOrNull { it.date == date.toString() }
            ?: return AssistantReply(
                "${TurkishText.capitalize(TurkishText.dayPhrase(date, today))} için henüz tahmin yok.",
                "Tahmin en fazla 16 gün ileriyi gösteriyor."
            )
        val chance = day.precipitationProbabilityMax
        val umbrella = if ((chance ?: 0) >= WeatherInsightBuilder.RAIN_PROBABILITY) "Şemsiyeni unutma." else null
        return AssistantReply(
            title = "${TurkishText.capitalize(
                TurkishText.dayPhrase(date, today)
            )} ${TurkishText.locative(city.name)} hava ${day.condition.label.lowercase(TurkishText.TR)}.",
            body = listOfNotNull(
                "En yüksek ${day.temperatureMax.deg()}°, en düşük ${day.temperatureMin.deg()}°.",
                chance?.let { "Yağış ihtimali en fazla %$it." },
                umbrella
            ).joinToString(" "),
            card = card("${day.temperatureMax.deg()}° / ${day.temperatureMin.deg()}° · ${day.condition.label}")
        )
    }

    private suspend fun upcomingPlans(): List<Plan> {
        val at = now()
        return data.plans().filter { !it.dateTime.plusHours(1).isBefore(at) }.sortedBy { it.dateTime }
    }

    private suspend fun plans(): AssistantReply {
        val upcoming = runCatching { upcomingPlans() }.getOrElse { return AssistantReply("Planlarını şu an okuyamadım.") }
        if (upcoming.isEmpty()) {
            return AssistantReply(
                "Yaklaşan planın yok.",
                "İstersen \"Yarın saat 6'da piknik planı oluştur\" diyebilirsin.",
                card = AssistantCard(CardKind.Plans, "Planlarım", "Henüz plan yok", VoiceDestination.Plans)
            )
        }
        val at = now()
        val forecasts = mutableMapOf<String, Forecast?>()
        val outlooks = upcoming.take(MAX_READ).map { plan ->
            val key = "%.3f,%.3f".format(java.util.Locale.ROOT, plan.city.latitude, plan.city.longitude)
            val forecast = forecasts.getOrPut(key) { loadForecast(plan.city).getOrNull() }
            plan to PlanAdvisor.outlook(plan, forecast, at)
        }
        val lines = outlooks.map { (plan, outlook) ->
            "${TurkishText.capitalize(
                TurkishText.dayPhrase(plan.localDate, at.toLocalDate())
            )} ${plan.time}, ${plan.title}: ${outlookPhrase(outlook)}."
        }
        val more = (upcoming.size - MAX_READ).takeIf { it > 0 }?.let { "Ve $it plan daha var." }
        val risky = outlooks.count { it.second is PlanOutlook.Risk }
        return AssistantReply(
            title = "${upcoming.size} yaklaşan planın var.",
            body = (lines + listOfNotNull(more)).joinToString(" "),
            card = AssistantCard(
                CardKind.Plans,
                "Planlarım",
                if (risky > 0) "$risky planda hava riski var" else "${upcoming.size} plan",
                VoiceDestination.Plans
            )
        )
    }

    private fun outlookPhrase(outlook: PlanOutlook): String = when (outlook) {
        is PlanOutlook.Risk -> outlook.reason.label.lowercase(TurkishText.TR)
        is PlanOutlook.Fine -> "hava uygun, ${outlook.hour.temperature.deg()}°"
        is PlanOutlook.NoForecastYet -> "tahmin henüz yok"
        PlanOutlook.Unknown -> "hava bilgisi alınamadı"
        PlanOutlook.Past -> "geçti"
    }

    private suspend fun outfit(dayOffset: Int): AssistantReply {
        val items = runCatching { data.wardrobe() }.getOrElse { return AssistantReply("Dolabını şu an okuyamadım.") }
        val today = now().toLocalDate()
        val date = today.plusDays(dayOffset.toLong())
        val city = data.homeCity()
        val forecast = loadForecast(city).getOrNull()
        val weather = forecast?.let { weatherContext(it, date, city.name) }
        val dayWord = TurkishText.dayPhrase(date, today)
        return when (val result = OutfitEngine.build(items, weather, date.monthValue, random, unit = data.temperatureUnit())) {
            is OutfitResult.Missing -> AssistantReply(
                "Kombin için dolabında yeterli parça yok.",
                "Eksik: ${result.categories.joinToString(", ") {
                    it.label.lowercase(TurkishText.TR)
                }}. Dolabım'dan fotoğrafla ekleyebilirsin.",
                card = AssistantCard(CardKind.Outfit, "Dolabım", "Parça ekle", VoiceDestination.Wardrobe)
            )
            is OutfitResult.Ready -> {
                val names = result.items.map { it.name }
                val list = if (names.size > 1) names.dropLast(1).joinToString(", ") + " ve " + names.last() else names.first()
                AssistantReply(
                    title = "${TurkishText.capitalize(dayWord)} için kombinin hazır.",
                    body = listOfNotNull("$list.", result.reason, result.note).joinToString(" "),
                    card = AssistantCard(
                        CardKind.Outfit,
                        "Salla Kombin",
                        result.items.joinToString(" · ") { it.category.short() },
                        VoiceDestination.Wardrobe
                    )
                )
            }
        }
    }

    private fun ClothingCategory.short() = when (this) {
        ClothingCategory.Top -> "Üst"
        ClothingCategory.Bottom -> "Alt"
        ClothingCategory.Dress -> "Elbise"
        ClothingCategory.Outerwear -> "Dış giyim"
        ClothingCategory.Shoes -> "Ayakkabı"
        ClothingCategory.Accessory -> "Aksesuar"
    }

    /** Daytime hours of [date] (or the rest of today) summed up for the outfit rules. */
    private fun weatherContext(forecast: Forecast, date: LocalDate, place: String): WeatherContext? {
        val at = now()
        val hours = forecast.hourly
            .filter { it.date == date.toString() && it.hour in 8..20 }
            .filter { date != at.toLocalDate() || it.hour >= at.hour }
            .ifEmpty { forecast.upcoming.take(6) }
        if (hours.isEmpty()) return null
        return WeatherContext(
            placeName = place,
            feelsLike = hours.map { it.apparentTemperature }.average(),
            rainLikely = hours.any { PlanAdvisor.risk(it) == com.kampplus.havacan.domain.plans.RiskReason.Rain },
            windKmh = hours.mapNotNull { it.windSpeed }.maxOrNull() ?: 0.0
        )
    }

    private suspend fun runTime(dayOffset: Int): AssistantReply {
        val city = data.homeCity()
        val forecast = loadForecast(city).getOrNull() ?: return noWeather()
        val at = now()
        var date = at.toLocalDate().plusDays(dayOffset.toLong())
        var outlook = runPlanner.outlook(forecast, date, RUN_MINUTES, at)
        var movedToTomorrow = false
        if (outlook == RunDayOutlook.NoHoursLeft && dayOffset == 0) {
            date = date.plusDays(1)
            outlook = runPlanner.outlook(forecast, date, RUN_MINUTES, at)
            movedToTomorrow = true
        }
        val dayWord = TurkishText.capitalize(TurkishText.dayPhrase(date, at.toLocalDate()))
        return when (outlook) {
            is RunDayOutlook.NoForecastYet -> AssistantReply("$dayWord için henüz tahmin yok.")
            RunDayOutlook.NoHoursLeft -> AssistantReply("Bugün için koşu saati kalmadı.")
            is RunDayOutlook.Ready -> {
                val best = outlook.suggestions.firstOrNull()
                    ?: return AssistantReply(
                        "$dayWord koşu için uygun saat görünmüyor.",
                        outlook.blocker?.message,
                        card = AssistantCard(CardKind.Run, "Koşu Planla", "Başka bir gün seç", VoiceDestination.RunPlanner)
                    )
                val h = best.first
                val quality = when {
                    best.score >= 85 -> "harika"
                    best.score >= 70 -> "iyi"
                    else -> "idare eder"
                }
                val wind = h.windSpeed?.roundToInt()?.let { if (it < 15) "Düşük rüzgar" else "Rüzgar $it km/sa" }
                AssistantReply(
                    title = "$dayWord ${"%02d:%02d".format(best.start.hour, best.start.minute)} en uygun saat!",
                    body = (if (movedToTomorrow) "Bugün için saat kalmadı, yarına baktım. " else "") +
                        "Hava ${h.temperature.deg()}° ve ${h.condition.label.lowercase(TurkishText.TR)}, koşu için $quality bir zaman.",
                    card = AssistantCard(
                        CardKind.Run,
                        if (best.score >= 70) "Koşu için ideal hava" else "Koşu için en iyi saat",
                        listOfNotNull("${h.temperature.deg()}°", h.condition.label, wind).joinToString(" · "),
                        VoiceDestination.RunPlanner
                    )
                )
            }
        }
    }

    private fun help() = AssistantReply(
        "Bana şunları söyleyebilirsin:",
        "\"Bugün hava nasıl?\", \"Planlarımı oku\", \"Yarın ne giyeyim?\", \"Koşu planla\", \"Dolabıma git\", \"Ana sayfaya dön\". " +
            "Plan da kurabilirim: \"Yarın saat 6'da piknik planı oluştur\". Kapatmak için \"Sesli rehberi kapat\" de."
    )

    /**
     * Hands a sentence the rules didn't understand to the AI. Its answer is text, or one tool from a
     * fixed list that is checked here and runs through the same confirmed flows as voice commands.
     * Null when no AI backend is configured.
     */
    private suspend fun unknown(text: String): AssistantReply? {
        val decision = runCatching { data.askAi(text, aiContext(), history.toList()) }
            .getOrElse {
                return AssistantReply(
                    "Yapay zekâ bağlantısına şu an ulaşamadım.",
                    "Hava, planlar, kombin ve koşu komutları yine çalışıyor."
                )
            }
            ?: return null
        decision.action?.let { return execute(it) }
        val reply = decision.reply?.trim()?.takeIf { it.isNotEmpty() } ?: return AssistantReply("Bunu anlayamadım.")
        return AssistantReply(reply.take(600))
    }

    /** Facts the AI may use: today, the home city's real forecast and the user's upcoming plans. */
    @Suppress("ktlint:standard:max-line-length")
    private suspend fun aiContext(): String {
        val at = now()
        val today = at.toLocalDate()
        val city = data.homeCity()
        val weekday = today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, TurkishText.TR)
        val forecast = loadForecast(city).getOrNull()
        val weather = forecast?.let { f ->
            val days = f.daily.take(4).joinToString("; ") {
                "${it.date}: ${it.condition.label}, ${it.temperatureMin.deg()}-${it.temperatureMax.deg()}${data.temperatureUnit().symbol}, yağış %${it.precipitationProbabilityMax ?: 0}"
            }
            "Şu an ${f.current.temperature.deg()}${data.temperatureUnit().symbol}, ${f.current.condition.label}. Günler: $days."
        } ?: "Hava verisi şu an yok."
        val plans = runCatching { upcomingPlans() }.getOrDefault(emptyList()).take(12).joinToString("\n") {
            "- id=${it.id} | ${it.title} | ${it.date} ${it.time} | ${it.city.name} | ${if (it.type == PlanType.Indoor) "kapalı" else "açık hava"}" +
                if (it.source == PlanSource.Calendar) " | takvimden" else ""
        }.ifEmpty { "Yok." }
        val clock = "%02d:%02d".format(at.hour, at.minute)
        return "Bugün: $today $weekday, saat $clock.\nAna şehir: ${city.name}. $weather\nYaklaşan planlar:\n$plans"
    }

    private suspend fun execute(action: AiAction): AssistantReply {
        val today = now().toLocalDate()
        val day = action.dayOffset.coerceIn(0, 15)
        return when (action.name) {
            "open_screen" -> {
                val to = VoiceDestination.entries.firstOrNull { it.name == action.screen } ?: return AssistantReply("O ekranı bulamadım.")
                navigate(VoiceCommand.Navigate(to, if (to == VoiceDestination.Plans) VoiceQuery.Plans else null))
            }
            "weather" -> weather(day)
            "list_plans" -> plans()
            "outfit" -> outfit(day)
            "run_time" -> runTime(day)
            "create_plan" -> nextStep(
                PlanDraft(
                    title = action.title?.trim()?.takeIf { it.length >= 2 }?.take(60),
                    date = parseDate(action.date, today),
                    time = parseTime(action.time),
                    type = if (action.indoor == true) PlanType.Indoor else PlanType.Outdoor
                ),
                today
            )
            "update_plan" -> startUpdate(action, today)
            "delete_plan" -> {
                val plan =
                    findPlan(action.planId)
                        ?: return AssistantReply("Hangi planı kastettiğini bulamadım.", "Planın adıyla tekrar söyler misin?")
                confirmDelete(plan)
            }
            else -> AssistantReply("Bunu yapamıyorum.")
        }
    }

    /** Only dates from today to a year ahead; anything else is asked again instead of guessed. */
    private fun parseDate(raw: String?, today: LocalDate): LocalDate? =
        raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.takeIf { !it.isBefore(today) && it.isBefore(today.plusYears(1)) }

    private fun parseTime(raw: String?): LocalTime? = raw?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    private suspend fun findPlan(id: String?): Plan? = id?.let { wanted ->
        runCatching { upcomingPlans() }.getOrNull()?.firstOrNull {
            it.id ==
                wanted
        }
    }

    // endregion

    // region Plans by voice: collect, read back, wait for "evet"

    private suspend fun startCreate(draft: PlanDraft, today: LocalDate): AssistantReply = nextStep(draft, today)

    private suspend fun continueCreate(p: Pending.Create, command: VoiceCommand, text: String, today: LocalDate): AssistantReply {
        if (command == VoiceCommand.No) return cancel("Tamam, planı kaydetmedim.")
        if (p.asking == null) {
            return when (command) {
                VoiceCommand.Yes -> save(p.draft)
                else -> AssistantReply("Planı kaydetmem için \"evet\", vazgeçmek için \"hayır\" de.")
            }
        }
        val heard = VoiceCommandParser.draft(text, today)
        val draft = when (p.asking) {
            Slot.Title -> p.draft.copy(
                title = heard.title ?: text.trim().takeIf { it.length >= 2 }?.let(TurkishText::capitalize),
                date = p.draft.date ?: heard.date,
                time = p.draft.time ?: heard.time
            )
            Slot.Date -> p.draft.copy(date = heard.date, time = p.draft.time ?: heard.time)
            Slot.Time -> p.draft.copy(time = heard.time, date = p.draft.date ?: heard.date)
        }
        val stillMissing = when (p.asking) {
            Slot.Title -> draft.title == null
            Slot.Date -> draft.date == null
            Slot.Time -> draft.time == null
        }
        if (stillMissing) {
            val retry = when (p.asking) {
                Slot.Title -> "Planın adını anlayamadım. Kısaca söyler misin? Örneğin \"Piknik\"."
                Slot.Date -> "Günü anlayamadım. \"Yarın\", \"Cumartesi\" ya da \"3 Kasım\" gibi söyleyebilirsin."
                Slot.Time -> "Saati anlayamadım. \"Saat 18\" ya da \"akşam 7\" gibi söyleyebilirsin."
            }
            return AssistantReply(retry)
        }
        return nextStep(draft, today)
    }

    private suspend fun nextStep(draft: PlanDraft, today: LocalDate): AssistantReply {
        val missing = when {
            draft.title == null -> Slot.Title
            draft.date == null -> Slot.Date
            draft.time == null -> Slot.Time
            else -> null
        }
        if (missing != null) {
            pending = Pending.Create(draft, missing)
            return AssistantReply(
                when (missing) {
                    Slot.Title -> "Planın adı ne olsun?"
                    Slot.Date -> "Hangi gün?"
                    Slot.Time -> "Saat kaçta?"
                }
            )
        }
        val date = draft.date!!
        val time = draft.time!!
        if (LocalDateTime.of(date, time).isBefore(now())) {
            pending = Pending.Create(draft.copy(time = null), Slot.Time)
            return AssistantReply("Bu saat geçmiş.", "Başka bir saat söyler misin?")
        }
        val plan = toPlan(draft)
        val forecast = loadForecast(plan.city).getOrNull()
        val weather = when (val o = PlanAdvisor.outlook(plan, forecast, now())) {
            is PlanOutlook.Risk -> "Dikkat: ${o.reason.label.lowercase(TurkishText.TR)}, ${o.hour.temperature.deg()}°."
            is PlanOutlook.Fine -> "Hava uygun görünüyor, ${o.hour.temperature.deg()}°."
            is PlanOutlook.NoForecastYet -> "O gün için tahmin henüz yok; yaklaşınca takip edeceğim."
            else -> "Hava bilgisini şu an alamadım."
        }
        pending = Pending.Create(draft, asking = null)
        return AssistantReply(
            title = "${TurkishText.capitalize(
                TurkishText.dayPhrase(date, today)
            )} saat ${plan.time}, ${plan.city.name}: \"${plan.title}\" planını oluşturacağım.",
            body = "$weather Onaylıyor musun? \"Evet\" ya da \"hayır\" de.",
            card = AssistantCard(
                CardKind.Plan,
                plan.title,
                "${date.dayOfMonth}.${"%02d".format(date.monthValue)} · ${plan.time} · ${plan.city.name}",
                null
            )
        )
    }

    private fun toPlan(draft: PlanDraft) = Plan(
        id = newId(),
        title = draft.title!!.take(60),
        date = draft.date!!.toString(),
        time = "%02d:%02d".format(draft.time!!.hour, draft.time.minute),
        city = data.homeCity(),
        type = draft.type,
        follow = true,
        reminder = Reminder.OneHour,
        source = PlanSource.App
    )

    private suspend fun save(draft: PlanDraft): AssistantReply {
        pending = null
        val plan = toPlan(draft)
        return runCatching { data.savePlan(plan) }.fold(
            onSuccess = {
                AssistantReply(
                    "Tamam, \"${plan.title}\" planını kaydettim.",
                    "Planlarım'a ekledim; havayı takip edip 1 saat önce hatırlatacağım.",
                    card = AssistantCard(
                        CardKind.Plan,
                        plan.title,
                        "${plan.time} · ${plan.city.name} · kaydedildi",
                        VoiceDestination.Plans
                    )
                )
            },
            onFailure = { AssistantReply("Planı kaydedemedim.", "Bir sorun oldu; Planlar ekranından tekrar deneyebilirsin.") }
        )
    }

    private suspend fun startDelete(name: String?): AssistantReply {
        val upcoming = runCatching { upcomingPlans() }.getOrElse { return AssistantReply("Planlarını şu an okuyamadım.") }
        if (upcoming.isEmpty()) return AssistantReply("Silinecek yaklaşan bir planın yok.")
        val wanted = name?.let(TurkishText::fold)?.split(' ')?.filter { it.length >= 2 }.orEmpty()
        val match =
            upcoming.firstOrNull { plan -> wanted.isNotEmpty() && wanted.all { w -> TurkishText.fold(plan.title).contains(w.take(5)) } }
                ?: return AssistantReply(
                    if (wanted.isEmpty()) "Hangi planı sileyim?" else "\"$name\" adında bir plan bulamadım.",
                    "Planın adıyla söyle: örneğin \"${upcoming.first().title} planını sil\"."
                )
        return confirmDelete(match)
    }

    private fun confirmDelete(match: Plan): AssistantReply {
        if (match.source == PlanSource.Calendar) {
            return AssistantReply("\"${match.title}\" takviminden geliyor.", "Takvim etkinliklerini takvim uygulamasından silebilirsin.")
        }
        pending = Pending.Delete(match)
        val today = now().toLocalDate()
        return AssistantReply(
            "${TurkishText.capitalize(
                TurkishText.dayPhrase(match.localDate, today)
            )} saat ${match.time} \"${match.title}\" planını sileceğim.",
            "Onaylıyor musun? \"Evet\" ya da \"hayır\" de.",
            card = AssistantCard(CardKind.Plan, match.title, "${match.time} · ${match.city.name}", null)
        )
    }

    /** A change the AI read from the sentence, read back in full before anything is saved. */
    private suspend fun startUpdate(action: AiAction, today: LocalDate): AssistantReply {
        val plan =
            findPlan(action.planId) ?: return AssistantReply("Hangi planı kastettiğini bulamadım.", "Planın adıyla tekrar söyler misin?")
        if (plan.source == PlanSource.Calendar) {
            return AssistantReply(
                "\"${plan.title}\" takviminden geliyor.",
                "Takvim etkinliklerini takvim uygulamasından değiştirebilirsin."
            )
        }
        val date = parseDate(action.date, today) ?: plan.localDate
        val time = parseTime(action.time) ?: plan.localTime
        val after = plan.copy(
            title = action.title?.trim()?.takeIf { it.length >= 2 }?.take(60) ?: plan.title,
            date = date.toString(),
            time = "%02d:%02d".format(time.hour, time.minute),
            type = when (action.indoor) {
                true -> PlanType.Indoor
                false -> PlanType.Outdoor
                null -> plan.type
            },
            lastNotified = null
        )
        if (after ==
            plan.copy(lastNotified = null)
        ) {
            return AssistantReply("Planda neyi değiştireyim?", "Örneğin \"bir saat ileri al\" ya da \"cumartesiye taşı\".")
        }
        if (after.dateTime.isBefore(now())) return AssistantReply("Bu saat geçmiş.", "Başka bir saat söyler misin?")
        pending = Pending.Update(plan, after)
        val changes = buildList {
            if (after.title != plan.title) add("adı \"${after.title}\"")
            if (after.date != plan.date ||
                after.time != plan.time
            ) {
                add("${TurkishText.dayPhrase(after.localDate, today)} saat ${after.time}")
            }
            if (after.type != plan.type) add(if (after.type == PlanType.Indoor) "kapalı alan" else "açık hava")
        }.joinToString(", ")
        val calendarNote = if (plan.calendarEventId != null) " Telefon takvimindeki etkinlik değişmez." else ""
        return AssistantReply(
            "\"${plan.title}\" planını değiştireceğim: $changes.",
            "Onaylıyor musun? \"Evet\" ya da \"hayır\" de.$calendarNote",
            card = AssistantCard(
                CardKind.Plan,
                after.title,
                "${after.localDate.dayOfMonth}.${"%02d".format(after.localDate.monthValue)} · ${after.time} · ${after.city.name}",
                null
            )
        )
    }

    private suspend fun continueUpdate(p: Pending.Update, command: VoiceCommand): AssistantReply = when (command) {
        VoiceCommand.Yes -> {
            pending = null
            runCatching { data.savePlan(p.after) }.fold(
                onSuccess = {
                    AssistantReply(
                        "Tamam, \"${p.after.title}\" planını güncelledim.",
                        card = AssistantCard(
                            CardKind.Plan,
                            p.after.title,
                            "${p.after.time} · ${p.after.city.name} · güncellendi",
                            VoiceDestination.Plans
                        )
                    )
                },
                onFailure = { AssistantReply("Planı güncelleyemedim.", "Planlar ekranından tekrar deneyebilirsin.") }
            )
        }
        VoiceCommand.No -> cancel("Tamam, planı değiştirmedim.")
        else -> AssistantReply("Değiştirmem için \"evet\", vazgeçmek için \"hayır\" de.")
    }

    private suspend fun continueDelete(p: Pending.Delete, command: VoiceCommand): AssistantReply = when (command) {
        VoiceCommand.Yes -> {
            pending = null
            runCatching { data.deletePlan(p.plan) }.fold(
                onSuccess = { AssistantReply("\"${p.plan.title}\" planını sildim.") },
                onFailure = { AssistantReply("Planı silemedim.", "Planlar ekranından tekrar deneyebilirsin.") }
            )
        }
        VoiceCommand.No -> cancel("Tamam, planı silmedim.")
        else -> AssistantReply("Silmem için \"evet\", vazgeçmek için \"hayır\" de.")
    }

    private fun cancel(message: String): AssistantReply {
        pending = null
        return AssistantReply(message)
    }

    /** Drops a half-finished plan dialog, e.g. when the voice session ends. */
    fun resetDialog() {
        pending = null
    }

    // endregion

    private companion object {
        const val MAX_READ = 3
        const val RUN_MINUTES = 45
        const val HISTORY = 6
    }
}
