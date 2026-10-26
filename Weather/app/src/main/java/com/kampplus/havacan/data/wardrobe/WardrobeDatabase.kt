package com.kampplus.havacan.data.wardrobe

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeDao {
    @Query("SELECT * FROM clothing ORDER BY createdAt DESC")
    fun observeItems(): Flow<List<ClothingItem>>

    @Query("SELECT * FROM clothing ORDER BY createdAt DESC")
    suspend fun items(): List<ClothingItem>

    @Query("SELECT * FROM clothing WHERE id = :id")
    suspend fun item(id: String): ClothingItem?

    @Upsert
    suspend fun upsert(item: ClothingItem)

    @Delete
    suspend fun delete(item: ClothingItem)

    @Query("SELECT * FROM saved_outfits ORDER BY createdAt DESC")
    fun observeOutfits(): Flow<List<SavedOutfit>>

    @Upsert
    suspend fun upsert(outfit: SavedOutfit)

    @Query("DELETE FROM saved_outfits WHERE id = :id")
    suspend fun deleteOutfit(id: String)
}

class WardrobeConverters {
    @TypeConverter
    fun seasonsToString(seasons: Set<Season>): String = seasons.joinToString(",") { it.name }

    @TypeConverter
    fun stringToSeasons(value: String): Set<Season> =
        value.split(',').filter { it.isNotBlank() }.mapNotNull { name -> Season.entries.firstOrNull { it.name == name } }.toSet()

    @TypeConverter
    fun idsToString(ids: List<String>): String = ids.joinToString(",")

    @TypeConverter
    fun stringToIds(value: String): List<String> = value.split(',').filter { it.isNotBlank() }
}

@Database(entities = [ClothingItem::class, SavedOutfit::class], version = 1, exportSchema = false)
@TypeConverters(WardrobeConverters::class)
abstract class WardrobeDatabase : RoomDatabase() {
    abstract fun wardrobe(): WardrobeDao

    companion object {
        fun create(context: Context): WardrobeDatabase = Room.databaseBuilder(context, WardrobeDatabase::class.java, "havacan.db").build()
    }
}
