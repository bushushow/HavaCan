package com.kampplus.havacan.data.wardrobe

import android.net.Uri
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Result of asking the AI about a photo; [Unavailable] means "fill the fields yourself". */
sealed interface AnalysisResult {
    data class Recognized(val guess: ClothingGuess) : AnalysisResult
    data class Unavailable(val reason: Reason) : AnalysisResult

    enum class Reason { NotConfigured, Failed }
}

class WardrobeRepository(
    private val dao: WardrobeDao,
    private val photos: PhotoStore,
    /** Null when no backend URL is configured. */
    private val analyzer: ClothingAnalyzer?
) {
    val items: Flow<List<ClothingItem>> = dao.observeItems()
    val outfits: Flow<List<SavedOutfit>> = dao.observeOutfits()

    val aiConfigured: Boolean get() = analyzer != null

    fun cameraFile() = photos.newCameraFile()

    /** Copies the photo into private storage; returns its permanent path. */
    suspend fun importPhoto(uri: Uri): String = photos.import(uri).absolutePath

    /** On-device, offline: the garment's main colour, if one stands out from the background. */
    suspend fun detectColor(path: String): NamedColor? {
        val (pixels, w, h) = photos.pixels(path) ?: return null
        return ColorDetector.dominantColor(pixels, w, h)?.let(ClothingColors::nearest)
    }

    suspend fun analyze(path: String): AnalysisResult {
        val analyzer = analyzer ?: return AnalysisResult.Unavailable(AnalysisResult.Reason.NotConfigured)
        val jpeg = photos.jpegForUpload(path) ?: return AnalysisResult.Unavailable(AnalysisResult.Reason.Failed)
        return runCatching { AnalysisResult.Recognized(analyzer.analyze(jpeg)) }
            .getOrElse { AnalysisResult.Unavailable(AnalysisResult.Reason.Failed) }
    }

    suspend fun item(id: String) = dao.item(id)

    suspend fun allItems() = dao.items()

    suspend fun save(item: ClothingItem) = dao.upsert(item)

    suspend fun delete(item: ClothingItem) {
        dao.delete(item)
        photos.delete(item.photoPath)
    }

    /** Deletes stored photos that no saved item uses; returns how many were removed. */
    suspend fun removeOrphanPhotos(): Int = photos.deleteOrphans(dao.items().map { it.photoPath }.toSet())

    /** Drops a photo that was imported but never saved (user backed out). */
    fun discardPhoto(path: String) = photos.delete(path)

    suspend fun saveOutfit(itemIds: List<String>, reason: String) =
        dao.upsert(SavedOutfit(UUID.randomUUID().toString(), itemIds, reason, System.currentTimeMillis()))

    suspend fun deleteOutfit(id: String) = dao.deleteOutfit(id)
}
