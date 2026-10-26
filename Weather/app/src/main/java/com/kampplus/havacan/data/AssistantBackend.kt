package com.kampplus.havacan.data

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import com.kampplus.havacan.data.wardrobe.ClothingItem
import com.kampplus.havacan.data.wardrobe.WardrobeRepository
import com.kampplus.havacan.domain.voice.AiAction
import com.kampplus.havacan.domain.voice.AiDecision
import com.kampplus.havacan.domain.voice.AiTurn
import com.kampplus.havacan.domain.voice.AssistantData
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.Plan
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Sentences the rules don't understand go to HavaCan's backend (backend/havacan-api,
 * POST /v1/assistant), which holds the OpenRouter key. It returns text or one tool call from a
 * fixed list; the assistant checks it and asks the user before anything is written.
 */
class BackendAssistant(private val baseUrl: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun decide(message: String, context: String, history: List<AiTurn>): AiDecision = withContext(Dispatchers.IO) {
        val body = gson.toJson(
            mapOf(
                "message" to message.take(500),
                "context" to context.take(2000),
                "history" to history.map { mapOf("role" to if (it.fromUser) "user" else "assistant", "content" to it.text.take(400)) }
            )
        )
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/v1/assistant")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            parse(response.body?.string().orEmpty())
        }
    }

    companion object {
        private data class ArgsDto(
            val screen: String?,
            @SerializedName("day_offset") val dayOffset: Int?,
            @SerializedName("plan_id") val planId: String?,
            val title: String?,
            val date: String?,
            val time: String?,
            val indoor: Boolean?
        )

        private data class ActionDto(val name: String?, val args: ArgsDto?)
        private data class Dto(val reply: String?, val action: ActionDto?)

        fun parse(json: String): AiDecision {
            val dto = try {
                Gson().fromJson(json, Dto::class.java)
            } catch (e: JsonParseException) {
                throw IOException("Unreadable AI response", e)
            } ?: throw IOException("Empty AI response")
            val action = dto.action?.name?.let { name ->
                val a = dto.action.args
                AiAction(
                    name = name,
                    screen = a?.screen,
                    dayOffset = a?.dayOffset ?: 0,
                    planId = a?.planId,
                    title = a?.title,
                    date = a?.date,
                    time = a?.time,
                    indoor = a?.indoor
                )
            }
            if (action == null && dto.reply.isNullOrBlank()) throw IOException("Empty AI response")
            return AiDecision(dto.reply, action)
        }
    }
}

/** The assistant's window onto the app: the same repositories the screens use, nothing separate. */
class AppAssistantData(
    private val location: LocationStore,
    private val weather: WeatherRepository,
    private val plansStore: PlanRepository,
    private val wardrobeRepo: WardrobeRepository,
    private val scheduler: PlanScheduler,
    private val ai: BackendAssistant?
) : AssistantData {
    override fun homeCity(): City = location.load() ?: DEFAULT_CITY

    override suspend fun forecast(city: City): Forecast? = weather.forecast(city.latitude, city.longitude)

    override suspend fun plans(): List<Plan> {
        plansStore.load()
        return plansStore.plans.value
    }

    override suspend fun savePlan(plan: Plan) {
        plansStore.upsert(plan)
        scheduler.schedule(plan)
    }

    override suspend fun deletePlan(plan: Plan) {
        plansStore.delete(plan.id)
        scheduler.cancel(plan.id)
    }

    override suspend fun wardrobe(): List<ClothingItem> = wardrobeRepo.allItems()

    override suspend fun askAi(message: String, context: String, history: List<AiTurn>): AiDecision? = ai?.decide(message, context, history)
}
