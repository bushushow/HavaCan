package com.kampplus.havacan.model

enum class ClothingCategory(val label: String) {
    Top("Üst giyim"),
    Bottom("Alt giyim"),
    Dress("Elbise / tulum"),
    Outerwear("Dış giyim"),
    Shoes("Ayakkabı"),
    Accessory("Aksesuar")
}

enum class Season(val label: String) {
    Spring("İlkbahar"),
    Summer("Yaz"),
    Autumn("Sonbahar"),
    Winter("Kış");

    companion object {
        /** Northern-hemisphere meteorological seasons. */
        fun ofMonth(month: Int): Season = when (month) {
            3, 4, 5 -> Spring
            6, 7, 8 -> Summer
            9, 10, 11 -> Autumn
            else -> Winter
        }
    }
}

/**
 * One piece of clothing the user photographed. [photoPath] is an absolute path inside the app's
 * private files, never a content URI, so the photo survives restarts and gallery clean-ups.
 * Plain Kotlin: the database row lives in data (ClothingEntity) and is mapped to this.
 */
data class ClothingItem(
    val id: String,
    val photoPath: String,
    val name: String,
    val category: ClothingCategory,
    val colorName: String,
    /** Empty means "any season". */
    val seasons: Set<Season>,
    val createdAt: Long
)

/** An outfit the user kept; it only references real [ClothingItem] ids. */
data class SavedOutfit(
    val id: String,
    val itemIds: List<String>,
    val reason: String,
    val createdAt: Long
)
