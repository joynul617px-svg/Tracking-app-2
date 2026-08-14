package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.example.model.NormalizedLandmark
import com.example.model.PoseFrameData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BodyPoseDetector(private val context: Context) {

    private val detector: PoseDetector by lazy {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    suspend fun detectPose(bitmap: Bitmap, timestampMs: Long): PoseFrameData? =
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                detector.process(image)
                    .addOnSuccessListener { pose ->
                        if (continuation.isActive) {
                            val frameData = parsePose(pose, bitmap.width, bitmap.height, timestampMs)
                            continuation.resume(frameData)
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

    private fun parsePose(pose: Pose, imageWidth: Int, imageHeight: Int, timestampMs: Long): PoseFrameData? {
        val allLandmarks = pose.allPoseLandmarks
        if (allLandmarks.isEmpty()) return null

        val landmarkMap = mutableMapOf<Int, NormalizedLandmark>()
        var minX = 1f
        var minY = 1f
        var maxX = 0f
        var maxY = 0f
        var totalLikelihood = 0f
        var count = 0

        var weightedSumX = 0f
        var weightedSumY = 0f

        for (landmark in allLandmarks) {
            val type = landmark.landmarkType
            val normX = (landmark.position.x / imageWidth).coerceIn(0f, 1f)
            val normY = (landmark.position.y / imageHeight).coerceIn(0f, 1f)
            val normZ = landmark.position3D.z / maxOf(imageWidth, imageHeight)
            val likelihood = landmark.inFrameLikelihood

            landmarkMap[type] = NormalizedLandmark(
                type = type,
                x = normX,
                y = normY,
                z = normZ,
                likelihood = likelihood
            )

            if (likelihood > 0.4f) {
                minX = minOf(minX, normX)
                minY = minOf(minY, normY)
                maxX = maxOf(maxX, normX)
                maxY = maxOf(maxY, normY)
                totalLikelihood += likelihood
                weightedSumX += normX * likelihood
                weightedSumY += normY * likelihood
                count++
            }
        }

        if (count == 0) return null

        val centerOfMassX = weightedSumX / totalLikelihood
        val centerOfMassY = weightedSumY / totalLikelihood
        val overallConfidence = (totalLikelihood / count).coerceIn(0f, 1f)

        // Expand bounding box slightly for natural body framing
        val paddingX = (maxX - minX) * 0.12f
        val paddingY = (maxY - minY) * 0.12f
        val bbox = RectF(
            (minX - paddingX).coerceIn(0f, 1f),
            (minY - paddingY).coerceIn(0f, 1f),
            (maxX + paddingX).coerceIn(0f, 1f),
            (maxY + paddingY).coerceIn(0f, 1f)
        )

        return PoseFrameData(
            timestampMs = timestampMs,
            landmarks = landmarkMap,
            boundingBox = bbox,
            centerOfMassX = centerOfMassX,
            centerOfMassY = centerOfMassY,
            overallConfidence = overallConfidence
        )
    }

    fun close() {
        detector.close()
    }
}
