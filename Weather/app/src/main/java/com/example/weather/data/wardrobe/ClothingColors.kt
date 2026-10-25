package com.example.weather.data.wardrobe

import kotlin.math.sqrt

/** A colour name the user can pick, with a representative RGB. */
data class NamedColor(val name: String, val rgb: Int, val neutral: Boolean)

object ClothingColors {
    val all: List<NamedColor> = listOf(
        NamedColor("Beyaz", 0xF5F5F2, neutral = true),
        NamedColor("Krem", 0xEDE3CF, neutral = true),
        NamedColor("Bej", 0xD2B48C, neutral = true),
        NamedColor("Gri", 0x8E9196, neutral = true),
        NamedColor("Siyah", 0x1E1E20, neutral = true),
        NamedColor("Lacivert", 0x1F2A4D, neutral = true),
        NamedColor("Kahverengi", 0x6B4A2F, neutral = true),
        NamedColor("Kot mavisi", 0x4F6F9A, neutral = true),
        NamedColor("Mavi", 0x2F6FDE, neutral = false),
        NamedColor("Açık mavi", 0x9EC5F0, neutral = false),
        NamedColor("Yeşil", 0x3E8E4E, neutral = false),
        NamedColor("Haki", 0x7A7A4A, neutral = false),
        NamedColor("Sarı", 0xF2C230, neutral = false),
        NamedColor("Turuncu", 0xEE7A2C, neutral = false),
        NamedColor("Kırmızı", 0xC9302C, neutral = false),
        NamedColor("Bordo", 0x6E1F2A, neutral = false),
        NamedColor("Pembe", 0xF1A0B8, neutral = false),
        NamedColor("Mor", 0x7A4BA8, neutral = false),
    )

    fun byName(name: String): NamedColor? = all.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun isNeutral(name: String): Boolean = byName(name)?.neutral ?: false

    /** Closest named colour to an RGB value (weighted RGB distance, good enough for clothing). */
    fun nearest(rgb: Int): NamedColor = all.minBy { distance(it.rgb, rgb) }

    internal fun distance(a: Int, b: Int): Double {
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
        val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return sqrt(2.0 * dr * dr + 4.0 * dg * dg + 3.0 * db * db)
    }
}

/**
 * Finds the garment's main colour on the device, no network: the most common border colour is
 * treated as background, and the most common colour bucket among centre pixels that differ from
 * it wins. When the garment fills the whole frame, the centre's own dominant colour is used.
 */
object ColorDetector {
    private const val BACKGROUND_DISTANCE = 36.0

    fun dominantColor(pixels: IntArray, width: Int, height: Int): Int? {
        if (width < 4 || height < 4 || pixels.size < width * height) return null
        val background = borderMode(pixels, width, height)
        return centreMode(pixels, width, height, background) ?: centreMode(pixels, width, height, null)
    }

    private fun centreMode(pixels: IntArray, width: Int, height: Int, background: Int?): Int? {
        val buckets = HashMap<Int, IntArray>() // bucket -> [count, r, g, b]
        val x0 = width / 6
        val x1 = width - width / 6
        val y0 = height / 6
        val y1 = height - height / 6
        for (y in y0 until y1 step 2) {
            for (x in x0 until x1 step 2) {
                val p = pixels[y * width + x] and 0xFFFFFF
                if (background != null && ClothingColors.distance(p, background) < BACKGROUND_DISTANCE) continue
                add(buckets, p)
            }
        }
        val best = buckets.values.maxByOrNull { it[0] } ?: return null
        // Ignore specks: the garment should fill a decent part of the centre
        val sampled = ((x1 - x0) / 2) * ((y1 - y0) / 2)
        if (best[0] < sampled * 0.04) return null
        return average(best)
    }

    private fun borderMode(pixels: IntArray, width: Int, height: Int): Int {
        val buckets = HashMap<Int, IntArray>()
        for (x in 0 until width) { add(buckets, pixels[x]); add(buckets, pixels[(height - 1) * width + x]) }
        for (y in 0 until height) { add(buckets, pixels[y * width]); add(buckets, pixels[y * width + width - 1]) }
        return average(buckets.values.maxBy { it[0] })
    }

    private fun add(buckets: HashMap<Int, IntArray>, pixel: Int) {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val key = (r shr 5 shl 6) or (g shr 5 shl 3) or (b shr 5)
        val acc = buckets.getOrPut(key) { IntArray(4) }
        acc[0]++; acc[1] += r; acc[2] += g; acc[3] += b
    }

    private fun average(acc: IntArray): Int =
        ((acc[1] / acc[0]) shl 16) or ((acc[2] / acc[0]) shl 8) or (acc[3] / acc[0])
}
