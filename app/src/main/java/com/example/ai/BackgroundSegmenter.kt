package com.example.ai

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.resume

class BackgroundSegmenter {

    private val segmenter: Segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
        Segmentation.getClient(options)
    }

    /**
     * Performs AI segmentation on the frame bitmap.
     * Returns a float mask bitmap where White (255) = Foreground Subject, Black (0) = Background,
     * with smooth alpha borders.
     */
    suspend fun generateMask(bitmap: Bitmap): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                segmenter.process(image)
                    .addOnSuccessListener { segmentationMask ->
                        if (continuation.isActive) {
                            val maskWidth = segmentationMask.width
                            val maskHeight = segmentationMask.height
                            val maskBuffer: ByteBuffer = segmentationMask.buffer

                            val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
                            val pixels = IntArray(maskWidth * maskHeight)

                            maskBuffer.rewind()
                            for (i in 0 until maskWidth * maskHeight) {
                                val confidence = maskBuffer.float
                                // confidence: 1.0 = subject, 0.0 = background
                                val alpha = (confidence * 255f).toInt().coerceIn(0, 255)
                                pixels[i] = Color.argb(alpha, 255, 255, 255)
                            }
                            maskBitmap.setPixels(pixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

                            // If mask size differs from input bitmap, scale it to match
                            val finalBitmap = if (maskWidth != bitmap.width || maskHeight != bitmap.height) {
                                val scaled = Bitmap.createScaledBitmap(maskBitmap, bitmap.width, bitmap.height, true)
                                maskBitmap.recycle()
                                scaled
                            } else {
                                maskBitmap
                            }

                            continuation.resume(finalBitmap)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }

    fun close() {
        segmenter.close()
    }
}
