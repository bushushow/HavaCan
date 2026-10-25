package com.example.weather.data.wardrobe

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** What the AI thinks a photo shows; every field is checked against the app's own lists. */
data class ClothingGuess(
    val name: String?,
    val category: ClothingCategory?,
    val colorName: String?,
    val seasons: Set<Season>,
)

fun interface ClothingAnalyzer {
    /** Throws on network/server problems; the caller falls back to manual entry. */
    suspend fun analyze(jpeg: ByteArray): ClothingGuess
}

/**
 * Talks to HavaCan's own backend (backend/havacan-api), which holds the OpenRouter key.
 * The app only knows the backend URL, so no secret ships in the APK or the repository.
 */
class BackendClothingAnalyzer(private val baseUrl: String) : ClothingAnalyzer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    override suspend fun analyze(jpeg: ByteArray): ClothingGuess = withContext(Dispatchers.IO) {
        val body = gson.toJson(mapOf("image" to Base64.encodeToString(jpeg, Base64.NO_WRAP)))
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/v1/clothing/analyze")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            parseGuess(response.body?.string().orEmpty())
        }
    }

    companion object {
        private data class Dto(val name: String?, val category: String?, val color: String?, val seasons: List<String>?)

        /** Accepts only known categories, colours and seasons; anything else becomes null/empty. */
        fun parseGuess(json: String): ClothingGuess {
            val dto = try {
                Gson().fromJson(json, Dto::class.java)
            } catch (e: JsonParseException) {
                throw IOException("Unreadable AI response", e)
            } ?: throw IOException("Empty AI response")
            return ClothingGuess(
                name = dto.name?.trim()?.take(40)?.takeIf { it.isNotBlank() },
                category = ClothingCategory.entries.firstOrNull { it.name.equals(dto.category, ignoreCase = true) },
                colorName = dto.color?.let { ClothingColors.byName(it)?.name },
                seasons = dto.seasons.orEmpty().mapNotNull { s -> Season.entries.firstOrNull { it.name.equals(s, ignoreCase = true) } }.toSet(),
            )
        }
    }
}
