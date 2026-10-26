package com.kampplus.havacan.data.wardrobe

import android.net.Uri
import com.kampplus.havacan.domain.wardrobe.ClothingColors
import com.kampplus.havacan.domain.wardrobe.ColorDetector
import com.kampplus.havacan.domain.wardrobe.NamedColor
import com.kampplus.havacan.model.ClothingItem
import com.kampplus.havacan.model.SavedOutfit
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Result of asking the AI about a photo; [Unavailable] means "fill the fields yourself". */
sealed interface AnalysisResult {
    data class Recognized(val guess: ClothingGuess) : AnalysisResult
    data class Unavailable(val reason: Reason) : AnalysisResult

    enum class Reason { NotConfigured, Failed }
}

/** The user's clothes and kept outfits, with their photos. */
interface WardrobeRepository {
    val items: Flow<List<ClothingItem>>
    val outfits: Flow<List<SavedOutfit>>
    val aiConfigured: Boolean

    fun cameraFile(): File

    /** Copies the photo into private storage; returns its permanent path. */
    suspend fun importPhoto(uri: Uri): String

    /** On-device, offline: the garment's main colour, if one stands out from the background. */
    suspend fun detectColor(path: String): NamedColor?
    suspend fun analyze(path: String): AnalysisResult
    suspend fun item(id: String): ClothingItem?
    suspend fun allItems(): List<ClothingItem>
    suspend fun save(item: ClothingItem)
    suspend fun delete(item: ClothingItem)

    /** Deletes stored photos that no saved item uses; returns how many were removed. */
    suspend fun removeOrphanPhotos(): Int

    /** Drops a photo that was imported but never saved (user backed out). */
    fun discardPhoto(path: String)
    suspend fun saveOutfit(itemIds: List<String>, reason: String)
    suspend fun deleteOutfit(id: String)
}

/** Room for the rows, private files for the photos, the backend (if configured) for AI labels. */
class RoomWardrobeRepository(
    private val dao: WardrobeDao,
    private val photos: PhotoStore,
    /** Null when no backend URL is configured. */
    private val analyzer: ClothingAnalyzer?
) : WardrobeRepository {
    override val items: Flow<List<ClothingItem>> = dao.observeItems().map { rows -> rows.map { it.toModel() } }
    override val outfits: Flow<List<SavedOutfit>> = dao.observeOutfits().map { rows -> rows.map { it.toModel() } }

    override val aiConfigured: Boolean get() = analyzer != null

    override fun cameraFile() = photos.newCameraFile()

    override suspend fun importPhoto(uri: Uri): String = photos.import(uri).absolutePath

    override suspend fun detectColor(path: String): NamedColor? {
        val (pixels, w, h) = photos.pixels(path) ?: return null
        return ColorDetector.dominantColor(pixels, w, h)?.let(ClothingColors::nearest)
    }

    override suspend fun analyze(path: String): AnalysisResult {
        val analyzer = analyzer ?: return AnalysisResult.Unavailable(AnalysisResult.Reason.NotConfigured)
        val jpeg = photos.jpegForUpload(path) ?: return AnalysisResult.Unavailable(AnalysisResult.Reason.Failed)
        return runCatching { AnalysisResult.Recognized(analyzer.analyze(jpeg)) }
            .getOrElse { AnalysisResult.Unavailable(AnalysisResult.Reason.Failed) }
    }

    override suspend fun item(id: String) = dao.item(id)?.toModel()

    override suspend fun allItems() = dao.items().map { it.toModel() }

    override suspend fun save(item: ClothingItem) = dao.upsert(item.toEntity())

    override suspend fun delete(item: ClothingItem) {
        dao.delete(item.toEntity())
        photos.delete(item.photoPath)
    }

    override suspend fun removeOrphanPhotos(): Int = photos.deleteOrphans(dao.items().map { it.photoPath }.toSet())

    override fun discardPhoto(path: String) {
        photos.delete(path)
    }

    override suspend fun saveOutfit(itemIds: List<String>, reason: String) =
        dao.upsert(SavedOutfitEntity(UUID.randomUUID().toString(), itemIds, reason, System.currentTimeMillis()))

    override suspend fun deleteOutfit(id: String) = dao.deleteOutfit(id)
}
