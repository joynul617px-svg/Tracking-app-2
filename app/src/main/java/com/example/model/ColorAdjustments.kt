package com.example.model

data class ColorAdjustments(
    val exposure: Float = 0.0f,     // -1.0 to 1.0
    val brightness: Float = 0.0f,   // -1.0 to 1.0
    val contrast: Float = 1.0f,     // 0.2 to 2.0
    val highlights: Float = 0.0f,   // -1.0 to 1.0
    val shadows: Float = 0.0f,      // -1.0 to 1.0
    val whites: Float = 0.0f,       // -1.0 to 1.0
    val blacks: Float = 0.0f,       // -1.0 to 1.0
    val saturation: Float = 1.0f,   // 0.0 to 2.0
    val vibrance: Float = 0.0f,     // -1.0 to 1.0
    val temperature: Float = 0.0f,  // -1.0 to 1.0 (Cool to Warm)
    val tint: Float = 0.0f,         // -1.0 to 1.0 (Green to Magenta)
    val sharpness: Float = 0.0f,    // 0.0 to 1.0
    val fade: Float = 0.0f,         // 0.0 to 1.0
    val vignette: Float = 0.0f,     // 0.0 to 1.0
    val grain: Float = 0.0f         // 0.0 to 1.0
) {
    val isDefault: Boolean
        get() = exposure == 0f && brightness == 0f && contrast == 1.0f &&
                highlights == 0f && shadows == 0f && whites == 0f && blacks == 0f &&
                saturation == 1.0f && vibrance == 0f && temperature == 0f && tint == 0f &&
                sharpness == 0f && fade == 0f && vignette == 0f && grain == 0f

    companion object {
        val DEFAULT = ColorAdjustments()
    }
}
