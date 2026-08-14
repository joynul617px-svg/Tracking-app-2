package com.example.processing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.ai.VirtualCameraController
import com.example.model.BackgroundEnhancement
import com.example.model.CameraKeyframe
import com.example.model.ColorAdjustments
import com.example.model.ExportConfig
import com.example.model.FilterSettings
import com.example.model.HslAdjustments
import com.example.model.PoseFrameData
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class VideoExportEngine(private val context: Context) {

    private var isCancelled = false

    fun cancel() {
        isCancelled = true
    }

    suspend fun exportVideo(
        sourceMetadata: VideoMetadata,
        keyframes: List<CameraKeyframe>,
        filterSettings: FilterSettings,
        colorAdjustments: ColorAdjustments,
        backgroundEnhancement: BackgroundEnhancement,
        hslAdjustments: HslAdjustments,
        poseMap: Map<Long, PoseFrameData>,
        exportConfig: ExportConfig,
        trimStartMs: Long,
        trimEndMs: Long,
        onProgress: (stage: String, percent: Int, detail: String) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        isCancelled = false

        val cameraController = VirtualCameraController()

        // 1. Determine Output Dimensions & Frame Rate
        val (targetWidth, targetHeight) = exportConfig.resolution.getDimensions(
            sourceMetadata.width,
            sourceMetadata.height
        )
        val targetFps = if (exportConfig.fps.fpsValue > 0) exportConfig.fps.fpsValue else sourceMetadata.fps.toInt().coerceIn(15, 60)
        val targetBitrate = exportConfig.calculateBitrate(targetWidth, targetHeight, targetFps)

        val durationMs = (if (trimEndMs > 0) trimEndMs else sourceMetadata.durationMs) - trimStartMs
        val totalFrames = ((durationMs / 1000f) * targetFps).toInt().coerceAtLeast(1)

        val tempFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.mp4")

        onProgress("Initializing Export Engine", 0, "${targetWidth}x${targetHeight} @ ${targetFps} FPS")

        val mimeType = "video/avc"
        val videoFormat = MediaFormat.createVideoFormat(mimeType, targetWidth, targetHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(mimeType)
        encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var muxerStarted = false

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, sourceMetadata.uri)
        } catch (e: Exception) {
            // Error opening source
        }

        val bufferInfo = MediaCodec.BufferInfo()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val dstRect = Rect(0, 0, targetWidth, targetHeight)

        try {
            for (frameIdx in 0 until totalFrames) {
                if (isCancelled) {
                    tempFile.delete()
                    return@withContext null
                }

                val progress = (frameIdx.toFloat() / totalFrames * 100).toInt()
                val currentTimestampMs = trimStartMs + (frameIdx.toFloat() / targetFps * 1000L).toLong()

                if (frameIdx % 5 == 0) {
                    val stageLabel = if (exportConfig.resolution.isUhd) "Exporting 4K UHD..." else "Rendering Video..."
                    onProgress(stageLabel, progress, "Frame ${frameIdx + 1} / $totalFrames")
                }

                // 1. Extract raw frame bitmap from source video
                val timeUs = currentTimestampMs * 1000L
                val rawFrame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                // 2. Evaluate Virtual Camera Keyframe at this timestamp
                val currentKeyframe = cameraController.evaluateKeyframeAt(currentTimestampMs, keyframes)

                // 3. Process frame with filters, color grading, virtual camera framing, and background enhancement
                val processedBitmap = if (rawFrame != null) {
                    ImageProcessingPipeline.processFrame(
                        sourceBitmap = rawFrame,
                        keyframe = currentKeyframe,
                        filterSettings = filterSettings,
                        colorAdjustments = colorAdjustments,
                        backgroundEnhancement = backgroundEnhancement,
                        hslAdjustments = hslAdjustments,
                        segmentationMask = null, // Proxy/Fast during export
                        targetWidth = targetWidth,
                        targetHeight = targetHeight
                    )
                } else {
                    Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                }

                // 4. Render to encoder input surface
                val canvas = inputSurface.lockCanvas(null)
                canvas.drawBitmap(processedBitmap, null, dstRect, paint)

                // Optional tracking skeleton overlay on export if user enabled it
                if (exportConfig.exportTrackingOverlay) {
                    val pose = poseMap[currentTimestampMs]
                    if (pose != null) {
                        drawTrackingOverlayOnCanvas(canvas, pose, targetWidth, targetHeight)
                    }
                }

                inputSurface.unlockCanvasAndPost(canvas)
                processedBitmap.recycle()
                rawFrame?.recycle()

                // 5. Drain encoder output buffers
                while (true) {
                    val status = encoder.dequeueOutputBuffer(bufferInfo, 8000)
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (status >= 0) {
                        val encodedBuffer = encoder.getOutputBuffer(status)
                        if (encodedBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedBuffer.position(bufferInfo.offset)
                            encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedBuffer, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(status, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }

            // Signal end of input
            encoder.signalEndOfInputStream()

            // Drain remaining encoder buffers
            var eos = false
            while (!eos) {
                val status = encoder.dequeueOutputBuffer(bufferInfo, 20000)
                if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                } else if (status >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                    val encodedBuffer = encoder.getOutputBuffer(status)
                    if (encodedBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedBuffer.position(bufferInfo.offset)
                        encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(status, false)
                }
            }
        } finally {
            try { encoder.stop() } catch (e: Exception) {}
            try { encoder.release() } catch (e: Exception) {}
            if (muxerStarted) {
                try { muxer.stop() } catch (e: Exception) {}
                try { muxer.release() } catch (e: Exception) {}
            }
            try { retriever.release() } catch (e: Exception) {}
        }

        if (isCancelled || !tempFile.exists() || tempFile.length() == 0L) {
            tempFile.delete()
            return@withContext null
        }

        // 6. Save to MediaStore Gallery
        onProgress("Saving to Gallery...", 98, "Writing MP4 to MediaStore")
        val savedUri = saveToMediaStore(tempFile, sourceMetadata.title)
        tempFile.delete()

        onProgress("Export Complete", 100, "Saved successfully to Gallery")
        savedUri
    }

    private fun drawTrackingOverlayOnCanvas(canvas: Canvas, pose: PoseFrameData, w: Int, h: Int) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(208, 188, 255)
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(74, 222, 128)
            style = Paint.Style.FILL
        }

        // Draw connections
        for ((p1Idx, p2Idx) in PoseFrameData.SKELETON_CONNECTIONS) {
            val p1 = pose.landmarks[p1Idx]
            val p2 = pose.landmarks[p2Idx]
            if (p1 != null && p2 != null && p1.likelihood > 0.4f && p2.likelihood > 0.4f) {
                canvas.drawLine(p1.x * w, p1.y * h, p2.x * w, p2.y * h, linePaint)
            }
        }

        // Draw joint nodes
        for (landmark in pose.landmarks.values) {
            if (landmark.likelihood > 0.4f) {
                canvas.drawCircle(landmark.x * w, landmark.y * h, 8f, nodePaint)
            }
        }
    }

    private fun saveToMediaStore(file: File, originalTitle: String): Uri? {
        val resolver = context.contentResolver
        val fileName = "AI_BodyTrack_${System.currentTimeMillis()}.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AIBodyTrack")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collection, values) ?: return null

        try {
            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(file).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            return itemUri
        } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            return null
        }
    }
}
