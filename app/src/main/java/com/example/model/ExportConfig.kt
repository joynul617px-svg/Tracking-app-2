package com.example.model

enum class ExportResolution(val label: String, val width: Int, val height: Int, val isUhd: Boolean = false) {
    ORIGINAL("Original Resolution", 0, 0),
    RES_480P("480p SD (854×480)", 854, 480),
    RES_720P("720p HD (1280×720)", 1280, 720),
    RES_1080P("1080p Full HD (1920×1080)", 1920, 1080),
    RES_1440P("1440p 2K QHD (2560×1440)", 2560, 1440),
    RES_4K("2160p 4K Ultra HD (3840×2160)", 3840, 2160, isUhd = true);

    fun getDimensions(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        if (this == ORIGINAL || width == 0 || height == 0) {
            return Pair(sourceWidth, sourceHeight)
        }
        val isPortrait = sourceHeight > sourceWidth
        return if (isPortrait) {
            val targetH = maxOf(width, height)
            val targetW = (targetH * (sourceWidth.toFloat() / sourceHeight)).toInt() and 1.inv() // make even
            Pair(maxOf(targetW, 320), targetH)
        } else {
            val targetW = maxOf(width, height)
            val targetH = (targetW * (sourceHeight.toFloat() / sourceWidth)).toInt() and 1.inv()
            Pair(targetW, maxOf(targetH, 240))
        }
    }
}

enum class ExportFps(val label: String, val fpsValue: Int) {
    ORIGINAL("Original FPS", 0),
    FPS_24("24 FPS (Cinematic)", 24),
    FPS_30("30 FPS (Standard)", 30),
    FPS_60("60 FPS (Smooth)", 60)
}

enum class ExportQuality(val label: String, val bitrateMultiplier: Float) {
    HIGH("High Quality (Crisp)", 1.4f),
    BALANCE("Best Balance", 1.0f),
    SMALL("Smaller File", 0.65f)
}

data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.RES_1080P,
    val fps: ExportFps = ExportFps.ORIGINAL,
    val quality: ExportQuality = ExportQuality.BALANCE,
    val muteAudio: Boolean = false,
    val exportTrackingOverlay: Boolean = false,
    val customBitrateMbps: Int? = null
) {
    fun calculateBitrate(width: Int, height: Int, fps: Int): Int {
        if (customBitrateMbps != null) {
            return customBitrateMbps * 1_000_000
        }
        val pixelRate = width.toLong() * height * fps
        val baseBps = when {
            pixelRate >= 3840L * 2160 * 30 -> 35_000_000 // 35 Mbps for 4K
            pixelRate >= 2560L * 1440 * 30 -> 18_000_000 // 18 Mbps for 2K
            pixelRate >= 1920L * 1080 * 30 -> 10_000_000 // 10 Mbps for 1080p
            pixelRate >= 1280L * 720 * 30 -> 5_000_000   // 5 Mbps for 720p
            else -> 2_500_000
        }
        return (baseBps * quality.bitrateMultiplier).toInt()
    }

    fun estimateFileSizeMb(durationSec: Float, width: Int, height: Int, fps: Int): Float {
        val bps = calculateBitrate(width, height, fps)
        val audioBps = if (muteAudio) 0 else 192_000
        val totalBits = (bps + audioBps) * durationSec
        return (totalBits / (8 * 1024 * 1024)).coerceAtLeast(0.1f)
    }
}
