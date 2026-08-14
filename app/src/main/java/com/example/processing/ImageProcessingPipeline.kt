package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.example.model.BackgroundEnhancement
import com.example.model.CameraKeyframe
import com.example.model.ColorAdjustments
import com.example.model.FilterSettings
import com.example.model.FilterType
import com.example.model.HslAdjustments
import com.example.model.HslColorChannel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object ImageProcessingPipeline {

    /**
     * Applies full editing stack to a source frame bitmap.
     */
    fun processFrame(
        sourceBitmap: Bitmap,
        keyframe: CameraKeyframe,
        filterSettings: FilterSettings,
        colorAdjustments: ColorAdjustments,
        backgroundEnhancement: BackgroundEnhancement,
        hslAdjustments: HslAdjustments,
        segmentationMask: Bitmap? = null,
        targetWidth: Int = sourceBitmap.width,
        targetHeight: Int = sourceBitmap.height
    ): Bitmap {
        // Step 1: Crop and Zoom based on Virtual Camera Keyframe
        val cropped = applyVirtualCameraCrop(sourceBitmap, keyframe, targetWidth, targetHeight)

        // Step 2: Pixel-level or ColorMatrix transforms
        val outputBitmap = cropped.copy(Bitmap.Config.ARGB_8888, true)
        if (cropped != sourceBitmap) {
            cropped.recycle()
        }

        // Apply AI Background enhancement if mask exists and enhancement is not default
        if (segmentationMask != null && (backgroundEnhancement.bgSaturation != 1.0f ||
                    backgroundEnhancement.bgVibrance != 0f ||
                    backgroundEnhancement.bgWarmth != 0f ||
                    backgroundEnhancement.bgBrightness != 0f ||
                    backgroundEnhancement.bgContrast != 1.0f ||
                    hslAdjustments.backgroundOnly)) {
            applyBackgroundMaskEnhancements(
                outputBitmap,
                segmentationMask,
                backgroundEnhancement,
                if (hslAdjustments.backgroundOnly) hslAdjustments else null
            )
        }

        // Apply Global Filters and Color Grading
        applyGlobalColorAndFilter(outputBitmap, filterSettings, colorAdjustments, if (!hslAdjustments.backgroundOnly) hslAdjustments else null)

        // Apply Vignette & Grain if set
        if (colorAdjustments.vignette > 0f || colorAdjustments.grain > 0f) {
            applyVignetteAndGrain(outputBitmap, colorAdjustments.vignette, colorAdjustments.grain)
        }

        return outputBitmap
    }

    /**
     * Extracts dynamic viewport for the virtual camera.
     */
    fun applyVirtualCameraCrop(
        source: Bitmap,
        keyframe: CameraKeyframe,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val srcW = source.width
        val srcH = source.height
        val zoom = keyframe.zoom.coerceIn(1.0f, 10.0f)

        // Calculate cropped window dimensions
        val cropW = (srcW / zoom).toInt().coerceIn(32, srcW)
        val cropH = (srcH / zoom).toInt().coerceIn(32, srcH)

        // Calculate window top-left based on focus center
        val centerX = (keyframe.focusX * srcW).toInt()
        val centerY = (keyframe.focusY * srcH).toInt()

        val left = (centerX - cropW / 2).coerceIn(0, srcW - cropW)
        val top = (centerY - cropH / 2).coerceIn(0, srcH - cropH)

        val srcRect = Rect(left, top, left + cropW, top + cropH)
        val dstBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dstBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val dstRect = Rect(0, 0, outWidth, outHeight)

        canvas.drawBitmap(source, srcRect, dstRect, paint)
        return dstBitmap
    }

    /**
     * Applies AI background segmentation enhancements: boosts background while protecting the subject.
     */
    private fun applyBackgroundMaskEnhancements(
        frame: Bitmap,
        mask: Bitmap,
        bgEnhancement: BackgroundEnhancement,
        hslBgOnly: HslAdjustments?
    ) {
        val width = frame.width
        val height = frame.height

        val scaledMask = if (mask.width != width || mask.height != height) {
            Bitmap.createScaledBitmap(mask, width, height, true)
        } else mask

        val pixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)

        frame.getPixels(pixels, 0, width, 0, 0, width, height)
        scaledMask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        val bgSat = bgEnhancement.bgSaturation
        val bgVib = bgEnhancement.bgVibrance
        val bgWarm = bgEnhancement.bgWarmth
        val bgBright = bgEnhancement.bgBrightness
        val bgCont = bgEnhancement.bgContrast
        val protectStrength = if (bgEnhancement.protectSubject) bgEnhancement.protectionStrength else 0f

        val hsv = FloatArray(3)

        for (i in pixels.indices) {
            val maskPixel = maskPixels[i]
            // Mask alpha / white channel: 1.0 (subject) to 0.0 (background)
            val subjectWeight = ((maskPixel and 0xFF) / 255f) * protectStrength
            val bgWeight = 1.0f - subjectWeight

            if (bgWeight <= 0.02f) continue // Subject fully protected

            var r = (pixels[i] shr 16) and 0xFF
            var g = (pixels[i] shr 8) and 0xFF
            var b = pixels[i] and 0xFF

            // Background Contrast & Brightness
            if (bgCont != 1.0f || bgBright != 0f) {
                var rf = (r / 255f - 0.5f) * (1f + (bgCont - 1f) * bgWeight) + 0.5f + bgBright * bgWeight
                var gf = (g / 255f - 0.5f) * (1f + (bgCont - 1f) * bgWeight) + 0.5f + bgBright * bgWeight
                var bf = (b / 255f - 0.5f) * (1f + (bgCont - 1f) * bgWeight) + 0.5f + bgBright * bgWeight
                r = (rf * 255f).toInt().coerceIn(0, 255)
                g = (gf * 255f).toInt().coerceIn(0, 255)
                b = (bf * 255f).toInt().coerceIn(0, 255)
            }

            // Background Warmth (shifts red/yellow up, blue down)
            if (bgWarm != 0f) {
                r = (r + bgWarm * 25f * bgWeight).toInt().coerceIn(0, 255)
                b = (b - bgWarm * 25f * bgWeight).toInt().coerceIn(0, 255)
            }

            // Background Saturation / Vibrance / HSL in HSV space
            if (bgSat != 1.0f || bgVib != 0f || hslBgOnly != null) {
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                var hue = hsv[0]
                var sat = hsv[1]
                var valLum = hsv[2]

                // Vibrance enhances lower saturation pixels more
                if (bgVib != 0f) {
                    sat += (1.0f - sat) * bgVib * bgWeight * 0.5f
                }
                // Saturation
                sat = (sat * (1.0f + (bgSat - 1.0f) * bgWeight)).coerceIn(0f, 1f)

                // Optional HSL for background only
                if (hslBgOnly != null) {
                    val channel = getChannelForHue(hue)
                    val adj = hslBgOnly.channels[channel]
                    if (adj != null) {
                        hue = (hue + adj.hue * 0.5f * bgWeight) % 360f
                        if (hue < 0f) hue += 360f
                        sat = (sat + (adj.saturation / 100f) * bgWeight * 0.5f).coerceIn(0f, 1f)
                        valLum = (valLum + (adj.luminance / 100f) * bgWeight * 0.5f).coerceIn(0f, 1f)
                    }
                }

                hsv[0] = hue
                hsv[1] = sat
                hsv[2] = valLum
                val newColor = android.graphics.Color.HSVToColor(hsv)
                r = (newColor shr 16) and 0xFF
                g = (newColor shr 8) and 0xFF
                b = newColor and 0xFF
            }

            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        frame.setPixels(pixels, 0, width, 0, 0, width, height)
        if (scaledMask != mask) {
            scaledMask.recycle()
        }
    }

    private fun getChannelForHue(hue: Float): HslColorChannel {
        return when {
            hue in 345f..360f || hue in 0f..15f -> HslColorChannel.RED
            hue in 15f..45f -> HslColorChannel.ORANGE
            hue in 45f..75f -> HslColorChannel.YELLOW
            hue in 75f..165f -> HslColorChannel.GREEN
            hue in 165f..195f -> HslColorChannel.CYAN
            hue in 195f..255f -> HslColorChannel.BLUE
            hue in 255f..295f -> HslColorChannel.PURPLE
            else -> HslColorChannel.MAGENTA
        }
    }

    /**
     * Applies filter color presets and pro color adjustments.
     */
    private fun applyGlobalColorAndFilter(
        frame: Bitmap,
        filterSettings: FilterSettings,
        adjustments: ColorAdjustments,
        hsl: HslAdjustments?
    ) {
        val width = frame.width
        val height = frame.height
        val pixels = IntArray(width * height)
        frame.getPixels(pixels, 0, width, 0, 0, width, height)

        val filterType = filterSettings.activeFilter
        val fIntensity = filterSettings.intensity.coerceIn(0f, 1f)

        // Extract adjustments
        val exposure = adjustments.exposure
        val brightness = adjustments.brightness
        val contrast = adjustments.contrast
        val highlights = adjustments.highlights
        val shadows = adjustments.shadows
        val saturation = adjustments.saturation
        val vibrance = adjustments.vibrance
        val temperature = adjustments.temperature
        val tint = adjustments.tint
        val fade = adjustments.fade

        val hsv = FloatArray(3)

        for (i in pixels.indices) {
            var r = (pixels[i] shr 16) and 0xFF
            var g = (pixels[i] shr 8) and 0xFF
            var b = pixels[i] and 0xFF

            // Apply filter preset matrix / math
            if (filterType != FilterType.NONE && fIntensity > 0.01f) {
                val filtered = applyFilterPresetToRgb(r, g, b, filterType, fIntensity)
                r = filtered[0]
                g = filtered[1]
                b = filtered[2]
            }

            // Exposure & Brightness
            if (exposure != 0f || brightness != 0f) {
                val mult = Math.pow(2.0, exposure.toDouble()).toFloat()
                r = ((r * mult) + brightness * 50f).toInt().coerceIn(0, 255)
                g = ((g * mult) + brightness * 50f).toInt().coerceIn(0, 255)
                b = ((b * mult) + brightness * 50f).toInt().coerceIn(0, 255)
            }

            // Contrast
            if (contrast != 1.0f) {
                r = (((r / 255f - 0.5f) * contrast + 0.5f) * 255f).toInt().coerceIn(0, 255)
                g = (((g / 255f - 0.5f) * contrast + 0.5f) * 255f).toInt().coerceIn(0, 255)
                b = (((b / 255f - 0.5f) * contrast + 0.5f) * 255f).toInt().coerceIn(0, 255)
            }

            // Shadows / Highlights
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            if (shadows != 0f && lum < 128f) {
                val shadowFactor = (1f - lum / 128f) * shadows * 40f
                r = (r + shadowFactor).toInt().coerceIn(0, 255)
                g = (g + shadowFactor).toInt().coerceIn(0, 255)
                b = (b + shadowFactor).toInt().coerceIn(0, 255)
            }
            if (highlights != 0f && lum >= 128f) {
                val highFactor = ((lum - 128f) / 127f) * highlights * 40f
                r = (r + highFactor).toInt().coerceIn(0, 255)
                g = (g + highFactor).toInt().coerceIn(0, 255)
                b = (b + highFactor).toInt().coerceIn(0, 255)
            }

            // Temperature (Warm/Cool) & Tint (Green/Magenta)
            if (temperature != 0f || tint != 0f) {
                r = (r + temperature * 25f + tint * 15f).toInt().coerceIn(0, 255)
                g = (g - tint * 20f).toInt().coerceIn(0, 255)
                b = (b - temperature * 25f + tint * 15f).toInt().coerceIn(0, 255)
            }

            // Fade (lifts black floor)
            if (fade > 0f) {
                val fadeOffset = fade * 35f
                r = (r * (1f - fade * 0.15f) + fadeOffset).toInt().coerceIn(0, 255)
                g = (g * (1f - fade * 0.15f) + fadeOffset).toInt().coerceIn(0, 255)
                b = (b * (1f - fade * 0.15f) + fadeOffset).toInt().coerceIn(0, 255)
            }

            // Saturation, Vibrance, HSL
            if (saturation != 1.0f || vibrance != 0f || hsl != null) {
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                var hue = hsv[0]
                var sat = hsv[1]
                var valLum = hsv[2]

                if (vibrance != 0f) {
                    sat += (1.0f - sat) * vibrance * 0.4f
                }
                sat = (sat * saturation).coerceIn(0f, 1f)

                if (hsl != null) {
                    val channel = getChannelForHue(hue)
                    val adj = hsl.channels[channel]
                    if (adj != null) {
                        hue = (hue + adj.hue * 0.5f) % 360f
                        if (hue < 0f) hue += 360f
                        sat = (sat + (adj.saturation / 100f) * 0.5f).coerceIn(0f, 1f)
                        valLum = (valLum + (adj.luminance / 100f) * 0.5f).coerceIn(0f, 1f)
                    }
                }

                hsv[0] = hue
                hsv[1] = sat
                hsv[2] = valLum
                val newColor = android.graphics.Color.HSVToColor(hsv)
                r = (newColor shr 16) and 0xFF
                g = (newColor shr 8) and 0xFF
                b = newColor and 0xFF
            }

            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        frame.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun applyFilterPresetToRgb(r: Int, g: Int, b: Int, type: FilterType, intensity: Float): IntArray {
        var tr = r.toFloat()
        var tg = g.toFloat()
        var tb = b.toFloat()

        when (type) {
            FilterType.NONE -> {}
            FilterType.NATURAL -> {
                // Natural: subtle contrast boost, neutral skin balance
                tr = (tr - 128f) * 1.05f + 128f + 3f
                tg = (tg - 128f) * 1.05f + 128f + 2f
                tb = (tb - 128f) * 1.05f + 128f
            }
            FilterType.CLEAN -> {
                // Clean: crisp whites, clean shadows
                tr = (tr - 128f) * 1.12f + 130f
                tg = (tg - 128f) * 1.12f + 130f
                tb = (tb - 128f) * 1.15f + 133f
            }
            FilterType.VIVID -> {
                // Vivid: punchy colors, warmer highlights
                tr = tr * 1.15f + 8f
                tg = tg * 1.05f + 4f
                tb = tb * 0.95f
            }
            FilterType.VIBRANT -> {
                // Vibrant: rich magenta/orange hues, vivid contrast
                tr = tr * 1.20f + 6f
                tg = tg * 1.08f
                tb = tb * 1.12f
            }
            FilterType.WARM -> {
                // Warm: golden tones
                tr = tr * 1.15f + 12f
                tg = tg * 1.05f + 4f
                tb = tb * 0.85f - 8f
            }
            FilterType.COOL -> {
                // Cool: cyan/blue atmosphere
                tr = tr * 0.88f - 5f
                tg = tg * 1.02f + 2f
                tb = tb * 1.20f + 15f
            }
            FilterType.CINEMATIC -> {
                // Cinematic: teal in shadows, orange in highlights
                val lum = 0.299f * tr + 0.587f * tg + 0.114f * tb
                if (lum < 128f) {
                    tg += (128f - lum) * 0.08f
                    tb += (128f - lum) * 0.18f
                } else {
                    tr += (lum - 128f) * 0.15f
                    tg += (lum - 128f) * 0.05f
                }
            }
            FilterType.SOFT -> {
                // Soft: lower contrast, gentle tones
                tr = (tr - 128f) * 0.85f + 132f
                tg = (tg - 128f) * 0.85f + 130f
                tb = (tb - 128f) * 0.88f + 132f
            }
            FilterType.DEEP -> {
                // Deep: deeper blacks, rich contrast
                tr = (tr - 128f) * 1.35f + 120f
                tg = (tg - 128f) * 1.35f + 120f
                tb = (tb - 128f) * 1.35f + 122f
            }
            FilterType.GOLDEN -> {
                // Golden: warm yellow/amber
                tr = tr * 1.22f + 14f
                tg = tg * 1.12f + 8f
                tb = tb * 0.80f - 10f
            }
            FilterType.SUNSET -> {
                // Sunset: orange/red highlights, rich shadows
                tr = tr * 1.28f + 18f
                tg = tg * 0.98f + 2f
                tb = tb * 0.78f - 12f
            }
            FilterType.PORTRAIT -> {
                // Portrait: soft highlights, flattering skin tones
                tr = (tr - 128f) * 0.98f + 132f + 6f
                tg = (tg - 128f) * 0.98f + 130f + 2f
                tb = (tb - 128f) * 0.98f + 128f
            }
            FilterType.FILM -> {
                // Film: analog matte curve, lifted blacks
                tr = (tr - 128f) * 0.90f + 135f
                tg = (tg - 128f) * 0.92f + 133f
                tb = (tb - 128f) * 0.88f + 136f
            }
            FilterType.MOODY -> {
                // Moody: deep shadows, desaturated cool tones
                tr = (tr - 128f) * 1.25f + 115f
                tg = (tg - 128f) * 1.20f + 115f
                tb = (tb - 128f) * 1.28f + 125f
            }
            FilterType.POP -> {
                // Pop: strong saturation & vibrant color separation
                tr = tr * 1.25f + 10f
                tg = tg * 1.15f + 5f
                tb = tb * 1.22f + 8f
            }
            FilterType.FRESH -> {
                // Fresh: bright greens & clean blues
                tr = tr * 0.96f
                tg = tg * 1.18f + 8f
                tb = tb * 1.15f + 10f
            }
        }

        // Blend with original using intensity
        val outR = (r + (tr - r) * intensity).toInt().coerceIn(0, 255)
        val outG = (g + (tg - g) * intensity).toInt().coerceIn(0, 255)
        val outB = (b + (tb - b) * intensity).toInt().coerceIn(0, 255)

        return intArrayOf(outR, outG, outB)
    }

    private fun applyVignetteAndGrain(bitmap: Bitmap, vignette: Float, grain: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val maxDist = kotlin.math.hypot(centerX, centerY)

        val random = Random(42)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                var r = (pixels[idx] shr 16) and 0xFF
                var g = (pixels[idx] shr 8) and 0xFF
                var b = pixels[idx] and 0xFF

                // Vignette: darkens edges
                if (vignette > 0f) {
                    val dist = kotlin.math.hypot(x - centerX, y - centerY)
                    val normDist = (dist / maxDist).coerceIn(0f, 1f)
                    val vigFactor = 1.0f - (normDist * normDist * vignette * 0.8f)
                    r = (r * vigFactor).toInt().coerceIn(0, 255)
                    g = (g * vigFactor).toInt().coerceIn(0, 255)
                    b = (b * vigFactor).toInt().coerceIn(0, 255)
                }

                // Grain: analog film noise
                if (grain > 0f) {
                    val noise = (random.nextFloat() - 0.5f) * grain * 60f
                    r = (r + noise).toInt().coerceIn(0, 255)
                    g = (g + noise).toInt().coerceIn(0, 255)
                    b = (b + noise).toInt().coerceIn(0, 255)
                }

                pixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
