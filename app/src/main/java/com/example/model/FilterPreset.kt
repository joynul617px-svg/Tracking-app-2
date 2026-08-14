package com.example.model

import androidx.compose.ui.graphics.Color

enum class FilterType(
    val displayName: String,
    val description: String,
    val previewGradientStart: Color,
    val previewGradientEnd: Color
) {
    NONE("Original", "No filter applied", Color(0xFF49454F), Color(0xFF2B2930)),
    NATURAL("Natural", "Realistic colors & skin tones", Color(0xFF65A30D), Color(0xFF84CC16)),
    CLEAN("Clean", "Clean whites & crisp details", Color(0xFF38BDF8), Color(0xFFE0F2FE)),
    VIVID("Vivid", "Stronger color & punchy contrast", Color(0xFFF97316), Color(0xFFFBBF24)),
    VIBRANT("Vibrant", "Enhanced vibrance & separation", Color(0xFFEC4899), Color(0xFF8B5CF6)),
    WARM("Warm", "Warm balance & golden highlights", Color(0xFFF59E0B), Color(0xFFD97706)),
    COOL("Cool", "Cooler tones & cyan atmosphere", Color(0xFF06B6D4), Color(0xFF3B82F6)),
    CINEMATIC("Cinematic", "Film-inspired contrast & shadows", Color(0xFF0F766E), Color(0xFF1E293B)),
    SOFT("Soft", "Gentle shadows & smooth look", Color(0xFFFBCFE8), Color(0xFFF472B6)),
    DEEP("Deep", "Deeper blacks & rich contrast", Color(0xFF1E1B4B), Color(0xFF4338CA)),
    GOLDEN("Golden", "Sunset gold & outdoor warmth", Color(0xFFFBBF24), Color(0xFFB45309)),
    SUNSET("Sunset", "Warm orange highlights & shadows", Color(0xFFEF4444), Color(0xFFEA580C)),
    PORTRAIT("Portrait", "Skin protection & subtle separation", Color(0xFFFB7185), Color(0xFFFDA4AF)),
    FILM("Film", "Controlled highlights & analog feel", Color(0xFF78716C), Color(0xFF44403C)),
    MOODY("Moody", "Deep atmosphere & controlled sat", Color(0xFF334155), Color(0xFF0F172A)),
    POP("Pop", "Vibrant colors & sharp separation", Color(0xFFA855F7), Color(0xFF06B6D4)),
    FRESH("Fresh", "Bright outdoor greens & clean blues", Color(0xFF10B981), Color(0xFF38BDF8))
}

data class FilterSettings(
    val activeFilter: FilterType = FilterType.NONE,
    val intensity: Float = 1.0f // 0.0 to 1.0
)
