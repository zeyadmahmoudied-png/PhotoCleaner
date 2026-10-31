package com.photocleaner.app.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream
import java.security.MessageDigest

object ImageHasher {

    private const val DHASH_SIZE = 8

    fun sha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int
        while (inputStream.read(buffer).also { read = it } > 0) {
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun dHash(bitmap: Bitmap): String {
        val resized = Bitmap.createScaledBitmap(bitmap, DHASH_SIZE + 1, DHASH_SIZE, true)
        val gray = Array(DHASH_SIZE) { IntArray(DHASH_SIZE + 1) }
        for (y in 0 until DHASH_SIZE) {
            for (x in 0 until DHASH_SIZE + 1) {
                val pixel = resized.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                gray[y][x] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }
        if (resized != bitmap) resized.recycle()

        val bits = StringBuilder(DHASH_SIZE * DHASH_SIZE)
        for (y in 0 until DHASH_SIZE) {
            for (x in 0 until DHASH_SIZE) {
                bits.append(if (gray[y][x] < gray[y][x + 1]) '1' else '0')
            }
        }
        return bits.toString()
    }

    fun hammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != hash2.length) return Int.MAX_VALUE
        var dist = 0
        for (i in hash1.indices) {
            if (hash1[i] != hash2[i]) dist++
        }
        return dist
    }

    fun similarityPercent(hash1: String, hash2: String): Int {
        val dist = hammingDistance(hash1, hash2)
        if (dist == Int.MAX_VALUE) return 0
        return ((64 - dist) * 100 / 64).coerceIn(0, 100)
    }

    fun decodeSampled(inputStream: InputStream, maxSide: Int = 256): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bmp = BitmapFactory.decodeStream(inputStream, null, options) ?: return null
            val w = bmp.width
            val h = bmp.height
            if (w <= maxSide && h <= maxSide) return bmp
            val scale = maxOf(w, h).toFloat() / maxSide
            val nw = (w / scale).toInt().coerceAtLeast(1)
            val nh = (h / scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bmp, nw, nh, true)
            if (scaled != bmp) bmp.recycle()
            scaled
        } catch (e: Exception) {
            null
        }
    }
}
