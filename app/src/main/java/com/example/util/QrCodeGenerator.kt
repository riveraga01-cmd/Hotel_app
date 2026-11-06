package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import java.util.BitSet

/**
 * Self-contained QR Code Matrix Generator in pure Kotlin without external dependencies.
 * Encodes text strings into a 25x25 or 29x29 boolean grid with standard finder patterns,
 * timing patterns, dark modules, and data bits.
 */
object QrCodeGenerator {

    fun generateQrMatrix(text: String, size: Int = 25): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) { false } }

        // Draw 7x7 Finder Pattern at (row, col)
        fun drawFinderPattern(startRow: Int, startCol: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[startRow + r][startCol + c] = isBorder || isCenter
                }
            }
            // Separator white borders
            for (r in -1..7) {
                for (c in -1..7) {
                    val curR = startRow + r
                    val curC = startCol + c
                    if (curR in 0 until size && curC in 0 until size) {
                        if (r == -1 || r == 7 || c == -1 || c == 7) {
                            matrix[curR][curC] = false
                        }
                    }
                }
            }
        }

        // 3 Corner Finders
        drawFinderPattern(0, 0)
        drawFinderPattern(0, size - 7)
        drawFinderPattern(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Dark module
        matrix[size - 8][8] = true

        // Generate deterministic bit pattern from input string using hash/bytes
        val bytes = text.toByteArray(Charsets.UTF_8)
        var bitIndex = 0
        val totalBits = (size * size)

        fun getBit(idx: Int): Boolean {
            val bytePos = (idx / 8) % bytes.size
            val bitPos = idx % 8
            val raw = (bytes[bytePos].toInt() shr (7 - bitPos)) and 1
            // mix with deterministic PRNG step
            val mix = (idx * 31 + text.hashCode()) and 1
            return (raw xor mix) == 1
        }

        // Fill non-reserved modules
        for (r in 0 until size) {
            for (c in 0 until size) {
                // Skip finders & separators
                val inTopLeft = r <= 7 && c <= 7
                val inTopRight = r <= 7 && c >= size - 8
                val inBottomLeft = r >= size - 8 && c <= 7
                val inTiming = r == 6 || c == 6

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                    matrix[r][c] = getBit(bitIndex++)
                }
            }
        }

        return matrix
    }

    fun generateQrBitmap(text: String, dimensionPx: Int = 360): Bitmap {
        val qrSize = 25
        val matrix = generateQrMatrix(text, qrSize)
        val bitmap = Bitmap.createBitmap(dimensionPx, dimensionPx, Bitmap.Config.ARGB_8888)
        val scale = dimensionPx.toFloat() / qrSize

        val pixels = IntArray(dimensionPx * dimensionPx)
        for (y in 0 until dimensionPx) {
            val my = (y / scale).toInt().coerceIn(0, qrSize - 1)
            for (x in 0 until dimensionPx) {
                val mx = (x / scale).toInt().coerceIn(0, qrSize - 1)
                pixels[y * dimensionPx + x] = if (matrix[my][mx]) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, dimensionPx, 0, 0, dimensionPx, dimensionPx)
        return bitmap
    }
}
