package com.example.ai

import android.graphics.PointF
import android.graphics.RectF
import com.example.model.PoseFrameData
import com.example.model.TrackingTarget
import kotlin.math.hypot

class TargetTracker {

    private var activeTarget: TrackingTarget = TrackingTarget(targetId = 1, isLocked = true)
    private var lastKnownPosition: PointF? = null
    private var velocityVector: PointF = PointF(0f, 0f)
    private val trajectoryHistory = mutableListOf<PointF>()
    private var maxHistorySize = 120

    fun getTarget(): TrackingTarget = activeTarget.copy(trajectoryPath = trajectoryHistory.toList())

    fun setLock(locked: Boolean) {
        activeTarget = activeTarget.copy(isLocked = locked)
    }

    fun selectTargetByTap(tapX: Float, tapY: Float, currentPose: PoseFrameData?) {
        activeTarget = activeTarget.copy(
            initialSelectionPoint = PointF(tapX, tapY),
            isLocked = true,
            occlusionCounter = 0
        )
        if (currentPose != null && currentPose.boundingBox.contains(tapX, tapY)) {
            lastKnownPosition = PointF(currentPose.centerOfMassX, currentPose.centerOfMassY)
        } else {
            lastKnownPosition = PointF(tapX, tapY)
        }
        trajectoryHistory.clear()
        trajectoryHistory.add(lastKnownPosition!!)
    }

    fun autoSelectTarget(detectedPose: PoseFrameData?) {
        if (detectedPose != null) {
            val center = PointF(detectedPose.centerOfMassX, detectedPose.centerOfMassY)
            activeTarget = activeTarget.copy(
                initialSelectionPoint = center,
                isLocked = true,
                occlusionCounter = 0
            )
            lastKnownPosition = center
            trajectoryHistory.clear()
            trajectoryHistory.add(center)
        }
    }

    fun updateFrame(detectedPose: PoseFrameData?): PointF {
        if (detectedPose == null) {
            // Target occluded or not detected in this frame
            val currentCounter = activeTarget.occlusionCounter + 1
            activeTarget = activeTarget.copy(
                isTrackingActive = currentCounter < 30, // active for ~1 sec of occlusion
                occlusionCounter = currentCounter
            )

            // Predict position with damped velocity
            val pos = lastKnownPosition ?: PointF(0.5f, 0.5f)
            val predicted = PointF(
                (pos.x + velocityVector.x * 0.5f).coerceIn(0.1f, 0.9f),
                (pos.y + velocityVector.y * 0.5f).coerceIn(0.1f, 0.9f)
            )
            velocityVector = PointF(velocityVector.x * 0.8f, velocityVector.y * 0.8f)
            lastKnownPosition = predicted
            return predicted
        }

        // We have a detected pose
        val currentCenter = PointF(detectedPose.centerOfMassX, detectedPose.centerOfMassY)
        val lastPos = lastKnownPosition

        if (lastPos != null) {
            val dist = hypot(currentCenter.x - lastPos.x, currentCenter.y - lastPos.y)
            if (dist < 0.35f || !activeTarget.isLocked) {
                // Match confirmed
                velocityVector = PointF(
                    (currentCenter.x - lastPos.x) * 0.5f + velocityVector.x * 0.5f,
                    (currentCenter.y - lastPos.y) * 0.5f + velocityVector.y * 0.5f
                )
                lastKnownPosition = currentCenter
                activeTarget = activeTarget.copy(
                    isTrackingActive = true,
                    occlusionCounter = 0
                )
            } else {
                // Large sudden jump (e.g. noise or different person); smooth towards it
                val smoothedX = lastPos.x + (currentCenter.x - lastPos.x) * 0.2f
                val smoothedY = lastPos.y + (currentCenter.y - lastPos.y) * 0.2f
                lastKnownPosition = PointF(smoothedX, smoothedY)
            }
        } else {
            lastKnownPosition = currentCenter
            activeTarget = activeTarget.copy(isTrackingActive = true, occlusionCounter = 0)
        }

        val point = lastKnownPosition!!
        trajectoryHistory.add(point)
        if (trajectoryHistory.size > maxHistorySize) {
            trajectoryHistory.removeAt(0)
        }

        return point
    }

    fun reset() {
        lastKnownPosition = null
        velocityVector = PointF(0f, 0f)
        trajectoryHistory.clear()
        activeTarget = TrackingTarget()
    }
}
