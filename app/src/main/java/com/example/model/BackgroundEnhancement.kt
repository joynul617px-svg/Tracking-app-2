package com.example.model

enum class BackgroundPreset(val displayName: String, val description: String) {
    NONE("Original", "Standard background"),
    GREEN_NATURE("Green Nature", "Enhance green trees & foliage"),
    VIVID_NATURE("Vivid Nature", "Deep sky & lush outdoor colors"),
    BLUE_SKY("Blue Sky", "Vibrant cyan & azure sky enhancement"),
    COLOR_POP("Color Pop", "High-contrast background punch"),
    GOLDEN_LANDSCAPE("Golden Landscape", "Sunset warmth on scenery"),
    COOL_LANDSCAPE("Cool Landscape", "Cyan/blue atmospheric grade"),
    CINEMATIC_BG("Cinematic BG", "Rich shadows and moody environment")
}

data class BackgroundEnhancement(
    val bgSaturation: Float = 1.0f,     // 0.0 to 2.0
    val bgVibrance: Float = 0.0f,       // -1.0 to 1.0
    val bgBrightness: Float = 0.0f,     // -1.0 to 1.0
    val bgContrast: Float = 1.0f,       // 0.2 to 2.0
    val bgWarmth: Float = 0.0f,         // -1.0 to 1.0
    val bgSharpness: Float = 0.0f,      // 0.0 to 1.0
    val bgHue: Float = 0.0f,            // -180f to 180f
    val protectSubject: Boolean = true,
    val protectionStrength: Float = 0.85f, // 0.0 to 1.0
    val activePreset: BackgroundPreset = BackgroundPreset.NONE
) {
    companion object {
        fun fromPreset(preset: BackgroundPreset, protect: Boolean = true, strength: Float = 0.85f): BackgroundEnhancement {
            return when (preset) {
                BackgroundPreset.NONE -> BackgroundEnhancement(protectSubject = protect, protectionStrength = strength, activePreset = preset)
                BackgroundPreset.GREEN_NATURE -> BackgroundEnhancement(
                    bgSaturation = 1.45f,
                    bgVibrance = 0.40f,
                    bgBrightness = 0.05f,
                    bgContrast = 1.15f,
                    bgWarmth = 0.10f,
                    bgSharpness = 0.30f,
                    bgHue = 10f,
                    protectSubject = protect,
                    protectionStrength = strength,
                    activePreset = preset
                )
                BackgroundPreset.VIVID_NATURE -> BackgroundEnhancement(
                    bgSaturation = 1.55f,
                    bgVibrance = 0.50f,
                    bgBrightness = 0.08f,
                    bgContrast = 1.20f,
                    bgWarmth = 0.05f,
                    bgSharpness = 0.35f,
                    bgHue = 0f,
                    protectSubject = protect,
                    protectionStrength = strength,
                    activePreset = preset
                )
                BackgroundPreset.BLUE_SKY -> BackgroundEnhancement(
                    bgSaturation = 1.40f,
                    bgVibrance = 0.45f,
                    bgBrightness = -0.05f,
                    bgContrast = 1.18f,
                    bgWarmth = -0.20f,
                    bgSharpness = 0.25f,
                    bgHue = -15f,
                    protectSubject = protect,
                    protectionStrength = strength,
                    activePreset = preset
                )
                BackgroundPreset.COLOR_POP -> BackgroundEnhancement(
                    bgSaturation = 1.65f,
                    bgVibrance = 0.60f,
                    bgBrightness = 0.05f,
                    bgContrast = 1.25f,
                    bgWarmth = 0.0f,
                    bgSharpness = 0.40f,
                    bgHue = 0f,
                    protectSubject = protect,
                    protectionStrength = strength,
                    activePreset = preset
                )
                BackgroundPreset.GOLDEN_LANDSCAPE -> BackgroundEnhancement(
                    bgSaturation = 1.35f,
                    bgVibrance = 0.35f,
                    bgBrightness = 0.10f,
                    bgContrast = 1.12f,
                    bgWarmth = 0.45f,
                    bgSharpness = 0.20f,
                    bgHue = 25f,
                    protectSubject = protect,
                    protectionStrength = strength,
                    activePreset = preset
                )
                BackgroundPreset.COOL_LANDSCAPE -> BackgroundEnhancement(
                    bgSaturation = 1.30f,
                    bgVibrance = 0.30f,
                    bgBrightness = -0.04f,
                    bgContrast = 1.15f,
                    bgWarmth = -0.40f,
                    bgSharpness = 0.25f,
                    bgHue = -20f,
                    protectSubject = protect,
                    protectionStrength = strength,
                    activePreset = preset
                )
                BackgroundPreset.CINEMATIC_BG -> BackgroundEnhancement(
                    bgSaturation = 1.20f,
                    bgVibrance = 0.20f,
                    bgBrightness = -0.10f,
                    bgContrast = 1.30f,
                    bgWarmth = -0.10f,
                    bgSharpness = 0.30f,
                    bgHue = -5f,
                    protectSubject = protect,
                    protectionStrength = strength,
                    activePreset = preset
                )
            }
        }
    }
}
