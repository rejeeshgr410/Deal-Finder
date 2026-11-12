package com.example.ui.loyalty

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

object BarcodeRenderer {

    // Code 128 Patterns (107 patterns, each pattern is 6 integers representing bar/space widths)
    private val CODE128_PATTERNS = arrayOf(
        intArrayOf(2, 1, 2, 2, 2, 2), intArrayOf(2, 2, 2, 1, 2, 2), intArrayOf(2, 2, 2, 2, 2, 1),
        intArrayOf(1, 2, 1, 2, 2, 3), intArrayOf(1, 2, 1, 3, 2, 2), intArrayOf(1, 3, 1, 2, 2, 2),
        intArrayOf(1, 2, 2, 2, 1, 3), intArrayOf(1, 2, 2, 3, 1, 2), intArrayOf(1, 3, 2, 2, 1, 2),
        intArrayOf(2, 2, 1, 2, 1, 3), intArrayOf(2, 2, 1, 3, 1, 2), intArrayOf(2, 3, 1, 2, 1, 2),
        intArrayOf(1, 1, 2, 2, 3, 2), intArrayOf(1, 2, 2, 1, 3, 2), intArrayOf(1, 2, 2, 2, 3, 1),
        intArrayOf(1, 1, 3, 2, 2, 2), intArrayOf(1, 2, 3, 1, 2, 2), intArrayOf(1, 2, 3, 2, 2, 1),
        intArrayOf(2, 2, 3, 2, 1, 1), intArrayOf(2, 2, 1, 1, 3, 2), intArrayOf(2, 2, 1, 2, 3, 1),
        intArrayOf(2, 1, 3, 2, 1, 2), intArrayOf(2, 2, 3, 1, 1, 2), intArrayOf(3, 1, 2, 1, 3, 1),
        intArrayOf(3, 1, 1, 2, 2, 2), intArrayOf(3, 2, 1, 1, 2, 2), intArrayOf(3, 2, 1, 2, 2, 1),
        intArrayOf(3, 1, 2, 2, 1, 2), intArrayOf(3, 2, 2, 1, 1, 2), intArrayOf(3, 2, 2, 2, 1, 1),
        intArrayOf(2, 1, 2, 1, 2, 3), intArrayOf(2, 1, 2, 3, 2, 1), intArrayOf(2, 3, 2, 1, 2, 1),
        intArrayOf(1, 1, 1, 3, 2, 3), intArrayOf(1, 3, 1, 1, 2, 3), intArrayOf(1, 3, 1, 3, 2, 1),
        intArrayOf(1, 1, 2, 3, 1, 3), intArrayOf(1, 3, 2, 1, 1, 3), intArrayOf(1, 3, 2, 3, 1, 1),
        intArrayOf(2, 1, 1, 3, 1, 3), intArrayOf(2, 3, 1, 1, 1, 3), intArrayOf(2, 3, 1, 3, 1, 1),
        intArrayOf(1, 1, 2, 1, 3, 3), intArrayOf(1, 1, 2, 3, 3, 1), intArrayOf(1, 3, 2, 1, 3, 1),
        intArrayOf(1, 1, 3, 1, 2, 3), intArrayOf(1, 1, 3, 3, 2, 1), intArrayOf(1, 3, 3, 1, 2, 1),
        intArrayOf(3, 1, 3, 1, 2, 1), intArrayOf(2, 1, 1, 3, 3, 1), intArrayOf(2, 3, 1, 1, 3, 1),
        intArrayOf(2, 1, 3, 1, 1, 3), intArrayOf(2, 1, 3, 3, 1, 1), intArrayOf(2, 1, 3, 1, 3, 1),
        intArrayOf(3, 1, 1, 1, 2, 3), intArrayOf(3, 1, 1, 3, 2, 1), intArrayOf(3, 3, 1, 1, 2, 1),
        intArrayOf(3, 1, 2, 1, 1, 3), intArrayOf(3, 1, 2, 3, 1, 1), intArrayOf(3, 3, 2, 1, 1, 1),
        intArrayOf(3, 1, 4, 1, 1, 1), intArrayOf(2, 2, 1, 4, 1, 1), intArrayOf(4, 3, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 2, 2, 4), intArrayOf(1, 1, 1, 4, 2, 2), intArrayOf(1, 2, 1, 1, 2, 4),
        intArrayOf(1, 2, 1, 4, 2, 1), intArrayOf(1, 4, 1, 1, 2, 2), intArrayOf(1, 4, 1, 2, 2, 1),
        intArrayOf(1, 1, 2, 2, 1, 4), intArrayOf(1, 1, 2, 4, 1, 2), intArrayOf(1, 2, 2, 1, 1, 4),
        intArrayOf(1, 2, 2, 4, 1, 1), intArrayOf(1, 4, 2, 1, 1, 2), intArrayOf(1, 4, 2, 2, 1, 1),
        intArrayOf(2, 4, 1, 2, 1, 1), intArrayOf(2, 2, 1, 1, 1, 4), intArrayOf(4, 1, 3, 1, 1, 1),
        intArrayOf(2, 4, 1, 1, 1, 2), intArrayOf(1, 3, 4, 1, 1, 1), intArrayOf(1, 1, 1, 2, 4, 2),
        intArrayOf(1, 2, 1, 1, 4, 2), intArrayOf(1, 2, 1, 2, 4, 1), intArrayOf(1, 1, 4, 2, 1, 2),
        intArrayOf(1, 2, 4, 1, 1, 2), intArrayOf(1, 2, 4, 2, 1, 1), intArrayOf(4, 1, 1, 2, 1, 2),
        intArrayOf(4, 2, 1, 1, 1, 2), intArrayOf(4, 2, 1, 2, 1, 1), intArrayOf(2, 1, 2, 1, 4, 1),
        intArrayOf(2, 1, 4, 1, 2, 1), intArrayOf(4, 1, 2, 1, 2, 1), intArrayOf(1, 1, 1, 1, 4, 3),
        intArrayOf(1, 1, 1, 3, 4, 1), intArrayOf(1, 3, 1, 1, 4, 1), intArrayOf(1, 1, 4, 1, 1, 3),
        intArrayOf(1, 1, 4, 3, 1, 1), intArrayOf(4, 1, 1, 1, 1, 3), intArrayOf(4, 1, 1, 3, 1, 1),
        intArrayOf(1, 1, 3, 1, 4, 1), intArrayOf(1, 1, 4, 1, 3, 1), intArrayOf(3, 1, 1, 1, 4, 1),
        intArrayOf(4, 1, 1, 1, 3, 1), intArrayOf(2, 1, 1, 4, 1, 2), intArrayOf(2, 1, 1, 2, 1, 4),
        intArrayOf(2, 1, 1, 2, 3, 2), intArrayOf(2, 3, 3, 1, 1, 1, 2) // 106 Stop (7 elements)
    )

    private const val START_CODE_B = 104
    private const val STOP_CODE = 106

    /**
     * Encodes arbitrary ASCII text into a Code-128B module array (true = black bar, false = white space).
     */
    fun encodeCode128(text: String): BooleanArray {
        val cleanText = if (text.isBlank()) "00000000" else text
        val codes = mutableListOf<Int>()
        codes.add(START_CODE_B)

        for (ch in cleanText) {
            val code = ch.code - 32
            if (code in 0..95) {
                codes.add(code)
            } else {
                codes.add(0) // space fallback
            }
        }

        // Checksum calculation: (StartCode + Sum(code_i * position_i)) % 103
        var checksum = START_CODE_B
        for (i in 1 until codes.size) {
            checksum += codes[i] * i
        }
        codes.add(checksum % 103)
        codes.add(STOP_CODE)

        // Convert widths to boolean array
        val modules = mutableListOf<Boolean>()
        // Quiet zone (10 modules)
        repeat(10) { modules.add(false) }

        for (c in codes) {
            val pattern = CODE128_PATTERNS[c]
            var isBar = true
            for (width in pattern) {
                repeat(width) { modules.add(isBar) }
                isBar = !isBar
            }
        }

        // Quiet zone (10 modules)
        repeat(10) { modules.add(false) }
        return modules.toBooleanArray()
    }

    /**
     * Generates a 2D boolean grid representing a clean QR-like pattern with authentic finder patterns
     */
    fun encodeQrMatrix(text: String, size: Int = 25): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) { false } }

        fun drawFinder(startX: Int, startY: Int) {
            for (y in 0 until 7) {
                for (x in 0 until 7) {
                    val isBorder = (x == 0 || x == 6 || y == 0 || y == 6)
                    val isCenter = (x in 2..4 && y in 2..4)
                    matrix[startY + y][startX + x] = isBorder || isCenter
                }
            }
        }

        // Three corner finder patterns
        drawFinder(0, 0)
        drawFinder(size - 7, 0)
        drawFinder(0, size - 7)

        // Timing patterns
        for (i in 8 until size - 8) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Data encoding mock based on hash
        val bytes = text.toByteArray(Charsets.UTF_8)
        var byteIdx = 0
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Avoid finder pattern zones
                val inTopLeft = x < 8 && y < 8
                val inTopRight = x >= size - 8 && y < 8
                val inBottomLeft = x < 8 && y >= size - 8
                val inTiming = x == 6 || y == 6

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                    val b = if (bytes.isNotEmpty()) bytes[byteIdx % bytes.size].toInt() else 42
                    matrix[y][x] = ((x * 3 + y * 7 + b + byteIdx) % 3 == 0)
                    byteIdx++
                }
            }
        }
        return matrix
    }

    /**
     * Creates an Android Bitmap from the barcode data
     */
    fun createBarcodeBitmap(data: String, format: String = "CODE_128", width: Int = 600, height: Int = 200): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (format.uppercase() == "QR_CODE") {
            val matrix = encodeQrMatrix(data, 29)
            val matrixSize = matrix.size
            val cellSize = width.coerceAtMost(height) / matrixSize
            val xOffset = (width - (matrixSize * cellSize)) / 2
            val yOffset = (height - (matrixSize * cellSize)) / 2

            for (y in 0 until height) {
                for (x in 0 until width) {
                    bitmap.setPixel(x, y, android.graphics.Color.WHITE)
                }
            }

            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (matrix[r][c]) {
                        for (py in 0 until cellSize) {
                            for (px in 0 until cellSize) {
                                val drawX = xOffset + (c * cellSize) + px
                                val drawY = yOffset + (r * cellSize) + py
                                if (drawX in 0 until width && drawY in 0 until height) {
                                    bitmap.setPixel(drawX, drawY, android.graphics.Color.BLACK)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val modules = encodeCode128(data)
            val moduleWidth = width.toFloat() / modules.size

            for (x in 0 until width) {
                val modIdx = (x / moduleWidth).toInt().coerceIn(0, modules.size - 1)
                val color = if (modules[modIdx]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, color)
                }
            }
        }
        return bitmap
    }
}

/**
 * Ultra-crisp vector Barcode Composable rendered natively via Canvas.
 */
@Composable
fun BarcodeView(
    data: String,
    format: String = "CODE_128",
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    if (format.uppercase() == "QR_CODE") {
        val matrix = remember(data) { BarcodeRenderer.encodeQrMatrix(data, 29) }
        Canvas(modifier = modifier) {
            drawRect(backgroundColor)
            val matrixSize = matrix.size
            val cellSize = size.minDimension / matrixSize
            val startX = (size.width - (matrixSize * cellSize)) / 2
            val startY = (size.height - (matrixSize * cellSize)) / 2

            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = barColor,
                            topLeft = Offset(startX + c * cellSize, startY + r * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    } else {
        val modules = remember(data) { BarcodeRenderer.encodeCode128(data) }
        Canvas(modifier = modifier) {
            drawRect(backgroundColor)
            val moduleWidth = size.width / modules.size
            for (i in modules.indices) {
                if (modules[i]) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(i * moduleWidth, 0f),
                        size = Size(moduleWidth + 0.5f, size.height)
                    )
                }
            }
        }
    }
}
