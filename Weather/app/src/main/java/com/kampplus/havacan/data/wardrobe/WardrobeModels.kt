package com.kampplus.havacan.data.wardrobe

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kampplus.havacan.model.ClothingCategory
import com.kampplus.havacan.model.ClothingItem
import com.kampplus.havacan.model.SavedOutfit
import com.kampplus.havacan.model.Season

/** The `clothing` table row. Same columns as before the model split, so no migration is needed. */
@Entity(tableName = "clothing")
data class ClothingEntity(
    @PrimaryKey val id: String,
    val photoPath: String,
    val name: String,
    val category: ClothingCategory,
    val colorName: String,
    val seasons: Set<Season>,
    val createdAt: Long
)

/** The `saved_outfits` table row. */
@Entity(tableName = "saved_outfits")
data class SavedOutfitEntity(
    @PrimaryKey val id: String,
    val itemIds: List<String>,
    val reason: String,
    val createdAt: Long
)

fun ClothingEntity.toModel() = ClothingItem(id, photoPath, name, category, colorName, seasons, createdAt)

fun ClothingItem.toEntity() = ClothingEntity(id, photoPath, name, category, colorName, seasons, createdAt)

fun SavedOutfitEntity.toModel() = SavedOutfit(id, itemIds, reason, createdAt)

fun SavedOutfit.toEntity() = SavedOutfitEntity(id, itemIds, reason, createdAt)
