package com.example.model

import androidx.compose.ui.graphics.Color

enum class HslColorChannel(val displayName: String, val baseColor: Color, val centerHue: Float) {
    RED("Red", Color(0xFFEF4444), 0f),
    ORANGE("Orange", Color(0xFFF97316), 30f),
    YELLOW("Yellow", Color(0xFFEAB308), 60f),
    GREEN("Green", Color(0xFF22C55E), 120f),
    CYAN("Cyan", Color(0xFF06B6D4), 180f),
    BLUE("Blue", Color(0xFF3B82F6), 240f),
    PURPLE("Purple", Color(0xFFA855F7), 280f),
    MAGENTA("Magenta", Color(0xFFEC4899), 320f)
}

data class HslChannelAdjustment(
    val hue: Float = 0f,        // -100f to +100f (maps to angle offset)
    val saturation: Float = 0f, // -100f to +100f
    val luminance: Float = 0f   // -100f to +100f
)

data class HslAdjustments(
    val channels: Map<HslColorChannel, HslChannelAdjustment> = HslColorChannel.entries.associateWith { HslChannelAdjustment() },
    val backgroundOnly: Boolean = false
) {
    val isDefault: Boolean
        get() = channels.values.all { it.hue == 0f && it.saturation == 0f && it.luminance == 0f }

    fun updateChannel(channel: HslColorChannel, update: (HslChannelAdjustment) -> HslChannelAdjustment): HslAdjustments {
        val current = channels[channel] ?: HslChannelAdjustment()
        val updatedMap = channels.toMutableMap().apply {
            put(channel, update(current))
        }
        return copy(channels = updatedMap)
    }

    companion object {
        val DEFAULT = HslAdjustments()
    }
}
