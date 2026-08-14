package com.example.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object SampleVideoGenerator {

    /**
     * Generates a sample MP4 video file with realistic human movements and outdoor background
     * for instant testing of AI Body Tracking, Virtual Camera Follow, and AI Segmentation.
     */
    suspend fun generateSampleVideo(
        context: Context,
        sampleType: String = "dance", // "dance" or "runner"
        onProgress: (Int) -> Unit = {}
    ): Uri = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "sample_videos").apply { mkdirs() }
        val outputFile = File(outputDir, "sample_${sampleType}_video.mp4")

        // Return cached sample if already generated
        if (outputFile.exists() && outputFile.length() > 50000) {
            return@withContext Uri.fromFile(outputFile)
        }

        val width = 1280
        val height = 720
        val fps = 30
        val durationSeconds = 6
        val totalFrames = fps * durationSeconds
        val bitrate = 4_000_000

        val mimeType = "video/avc"
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(mimeType)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = codec.createInputSurface()
        codec.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        try {
            for (frameIndex in 0 until totalFrames) {
                val t = frameIndex.toFloat() / totalFrames
                val timeSec = frameIndex.toFloat() / fps

                // Render dynamic frame
                val canvas = inputSurface.lockCanvas(null)
                drawSampleScene(canvas, width, height, timeSec, sampleType, paint)
                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder
                while (true) {
                    val status = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (status >= 0) {
                        val encodedBuffer = codec.getOutputBuffer(status)
                        if (encodedBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedBuffer.position(bufferInfo.offset)
                            encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(status, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }

                if (frameIndex % 5 == 0) {
                    onProgress((t * 100).toInt())
                }
            }

            // Signal End of Stream
            codec.signalEndOfInputStream()

            // Drain remaining frames
            var eos = false
            while (!eos) {
                val status = codec.dequeueOutputBuffer(bufferInfo, 20000)
                if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                } else if (status >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                    val encodedBuffer = codec.getOutputBuffer(status)
                    if (encodedBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedBuffer.position(bufferInfo.offset)
                        encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedBuffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(status, false)
                }
            }
        } finally {
            try { codec.stop() } catch (e: Exception) {}
            try { codec.release() } catch (e: Exception) {}
            if (muxerStarted) {
                try { muxer.stop() } catch (e: Exception) {}
                try { muxer.release() } catch (e: Exception) {}
            }
        }

        Uri.fromFile(outputFile)
    }

    fun generateDirectFrameBitmap(
        sampleType: String = "dance",
        timeSec: Float,
        width: Int = 1280,
        height: Int = 720
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawSampleScene(canvas, width, height, timeSec, sampleType, paint)
        return bitmap
    }

    private fun drawSampleScene(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeSec: Float,
        sampleType: String,
        paint: Paint
    ) {
        // 1. Draw Outdoor Background (Sky, Sun, Mountains, Green Grass/Foliage)
        // Sky
        paint.color = Color.rgb(65, 145, 235)
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.65f, paint)

        // Sun
        paint.color = Color.rgb(255, 220, 80)
        canvas.drawCircle(width * 0.85f, height * 0.22f, 60f, paint)

        // Mountains
        paint.color = Color.rgb(85, 110, 140)
        val mountainPath = Path().apply {
            moveTo(0f, height * 0.65f)
            lineTo(width * 0.25f, height * 0.35f)
            lineTo(width * 0.5f, height * 0.55f)
            lineTo(width * 0.75f, height * 0.30f)
            lineTo(width.toFloat(), height * 0.65f)
            close()
        }
        canvas.drawPath(mountainPath, paint)

        // Lush Green Trees & Hills
        paint.color = Color.rgb(34, 140, 50)
        canvas.drawRect(0f, height * 0.60f, width.toFloat(), height.toFloat(), paint)

        // Trees
        paint.color = Color.rgb(20, 110, 35)
        for (treeX in listOf(120f, 260f, 400f, 880f, 1050f, 1180f)) {
            canvas.drawCircle(treeX, height * 0.58f, 75f, paint)
        }

        // 2. Draw Moving Human Body (Dancer / Runner)
        // Horizontal movement cycle across screen
        val cycleProgress = (timeSec % 4f) / 4f
        val personCenterX = width * (0.25f + 0.50f * (sin(timeSec * 1.5).toFloat() * 0.5f + 0.5f))
        val bounce = abs(sin(timeSec * 6.0).toFloat()) * 25f
        val personGroundY = height * 0.82f - bounce
        val personHeight = height * 0.48f

        val headRadius = personHeight * 0.09f
        val headCenterY = personGroundY - personHeight + headRadius

        // Head (Skin tone)
        paint.color = Color.rgb(240, 195, 160)
        canvas.drawCircle(personCenterX, headCenterY, headRadius, paint)

        // Hair
        paint.color = Color.rgb(40, 30, 25)
        canvas.drawArc(
            RectF(personCenterX - headRadius, headCenterY - headRadius, personCenterX + headRadius, headCenterY + headRadius * 0.5f),
            180f, 180f, true, paint
        )

        // Torso / T-shirt (Vibrant Blue/Cyan shirt)
        paint.color = Color.rgb(220, 50, 80)
        val torsoTop = headCenterY + headRadius * 1.1f
        val torsoBottom = torsoTop + personHeight * 0.38f
        val torsoWidth = personHeight * 0.22f
        canvas.drawRoundRect(
            RectF(personCenterX - torsoWidth / 2, torsoTop, personCenterX + torsoWidth / 2, torsoBottom),
            15f, 15f, paint
        )

        // Arms (Swaying with rhythm)
        paint.color = Color.rgb(240, 195, 160)
        paint.strokeWidth = 18f
        paint.strokeCap = Paint.Cap.ROUND

        val leftArmAngle = sin(timeSec * 5.0).toFloat() * 0.8f
        val rightArmAngle = -cos(timeSec * 5.0).toFloat() * 0.8f

        // Left Arm
        val leftElbowX = personCenterX - torsoWidth / 2 - 25f + sin(leftArmAngle) * 35f
        val leftElbowY = torsoTop + 40f + cos(leftArmAngle) * 35f
        val leftHandX = leftElbowX - 25f + sin(leftArmAngle * 1.2f) * 35f
        val leftHandY = leftElbowY + 30f + cos(leftArmAngle * 1.2f) * 35f
        canvas.drawLine(personCenterX - torsoWidth / 2, torsoTop + 15f, leftElbowX, leftElbowY, paint)
        canvas.drawLine(leftElbowX, leftElbowY, leftHandX, leftHandY, paint)

        // Right Arm
        val rightElbowX = personCenterX + torsoWidth / 2 + 25f + sin(rightArmAngle) * 35f
        val rightElbowY = torsoTop + 40f + cos(rightArmAngle) * 35f
        val rightHandX = rightElbowX + 25f + sin(rightArmAngle * 1.2f) * 35f
        val rightHandY = rightElbowY + 30f + cos(rightArmAngle * 1.2f) * 35f
        canvas.drawLine(personCenterX + torsoWidth / 2, torsoTop + 15f, rightElbowX, rightElbowY, paint)
        canvas.drawLine(rightElbowX, rightElbowY, rightHandX, rightHandY, paint)

        // Legs / Pants (Dark jeans)
        paint.color = Color.rgb(35, 45, 65)
        paint.strokeWidth = 24f

        val legCycle = timeSec * 6.0f
        val leftLegAngle = sin(legCycle) * 0.6f
        val rightLegAngle = -sin(legCycle) * 0.6f

        // Left Leg
        val leftKneeX = personCenterX - torsoWidth * 0.25f + sin(leftLegAngle) * 50f
        val leftKneeY = torsoBottom + cos(leftLegAngle) * 60f
        val leftFootX = leftKneeX + sin(leftLegAngle * 1.1f) * 50f
        val leftFootY = personGroundY
        canvas.drawLine(personCenterX - torsoWidth * 0.25f, torsoBottom, leftKneeX, leftKneeY, paint)
        canvas.drawLine(leftKneeX, leftKneeY, leftFootX, leftFootY, paint)

        // Right Leg
        val rightKneeX = personCenterX + torsoWidth * 0.25f + sin(rightLegAngle) * 50f
        val rightKneeY = torsoBottom + cos(rightLegAngle) * 60f
        val rightFootX = rightKneeX + sin(rightLegAngle * 1.1f) * 50f
        val rightFootY = personGroundY
        canvas.drawLine(personCenterX + torsoWidth * 0.25f, torsoBottom, rightKneeX, rightKneeY, paint)
        canvas.drawLine(rightKneeX, rightKneeY, rightFootX, rightFootY, paint)

        // Shoes
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawCircle(leftFootX, leftFootY, 14f, paint)
        canvas.drawCircle(rightFootX, rightFootY, 14f, paint)

        // Reset paint
        paint.strokeCap = Paint.Cap.BUTT
        paint.strokeWidth = 0f
    }
}
