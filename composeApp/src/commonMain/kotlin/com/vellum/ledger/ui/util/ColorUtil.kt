package com.vellum.ledger.ui.util

import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String): Color {
    return try {
        val colorHex = hex.removePrefix("#")
        val colorLong = colorHex.toLong(16)
        if (colorHex.length == 6) {
            Color(0xFF000000 or colorLong)
        } else {
            Color(colorLong)
        }
    } catch (e: Exception) {
        Color.Gray
    }
}
