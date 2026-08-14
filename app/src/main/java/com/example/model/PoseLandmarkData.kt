package com.example.model

import android.graphics.RectF

/**
 * Normalized 2D/3D body landmark coordinate (x, y normalized to [0, 1]).
 */
data class NormalizedLandmark(
    val type: Int,
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val likelihood: Float = 1.0f
)

/**
 * Full body pose data for a specific video timestamp.
 */
data class PoseFrameData(
    val timestampMs: Long,
    val landmarks: Map<Int, NormalizedLandmark>,
    val boundingBox: RectF,
    val centerOfMassX: Float,
    val centerOfMassY: Float,
    val overallConfidence: Float,
    val detectedPersonId: Int = 0
) {
    // Landmark index constants matching ML Kit PoseLandmark
    companion object {
        const val NOSE = 0
        const val LEFT_EYE_INNER = 1
        const val LEFT_EYE = 2
        const val LEFT_EYE_OUTER = 3
        const val RIGHT_EYE_INNER = 4
        const val RIGHT_EYE = 5
        const val RIGHT_EYE_OUTER = 6
        const val LEFT_EAR = 7
        const val RIGHT_EAR = 8
        const val LEFT_MOUTH = 9
        const val RIGHT_MOUTH = 10
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_ELBOW = 13
        const val RIGHT_ELBOW = 14
        const val LEFT_WRIST = 15
        const val RIGHT_WRIST = 16
        const val LEFT_PINKY = 17
        const val RIGHT_PINKY = 18
        const val LEFT_INDEX = 19
        const val RIGHT_INDEX = 20
        const val LEFT_THUMB = 21
        const val RIGHT_THUMB = 22
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_KNEE = 25
        const val RIGHT_KNEE = 26
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
        const val LEFT_HEEL = 29
        const val RIGHT_HEEL = 30
        const val LEFT_FOOT_INDEX = 31
        const val RIGHT_FOOT_INDEX = 32

        // Skeletal bone connections for rendering the body skeleton
        val SKELETON_CONNECTIONS = listOf(
            // Face
            NOSE to LEFT_EYE, LEFT_EYE to LEFT_EAR,
            NOSE to RIGHT_EYE, RIGHT_EYE to RIGHT_EAR,
            LEFT_MOUTH to RIGHT_MOUTH,
            // Torso / Shoulders
            LEFT_SHOULDER to RIGHT_SHOULDER,
            LEFT_SHOULDER to LEFT_HIP,
            RIGHT_SHOULDER to RIGHT_HIP,
            LEFT_HIP to RIGHT_HIP,
            // Left Arm
            LEFT_SHOULDER to LEFT_ELBOW,
            LEFT_ELBOW to LEFT_WRIST,
            LEFT_WRIST to LEFT_THUMB,
            LEFT_WRIST to LEFT_INDEX,
            LEFT_WRIST to LEFT_PINKY,
            // Right Arm
            RIGHT_SHOULDER to RIGHT_ELBOW,
            RIGHT_ELBOW to RIGHT_WRIST,
            RIGHT_WRIST to RIGHT_THUMB,
            RIGHT_WRIST to RIGHT_INDEX,
            RIGHT_WRIST to RIGHT_PINKY,
            // Left Leg
            LEFT_HIP to LEFT_KNEE,
            LEFT_KNEE to LEFT_ANKLE,
            LEFT_ANKLE to LEFT_HEEL,
            LEFT_HEEL to LEFT_FOOT_INDEX,
            // Right Leg
            RIGHT_HIP to RIGHT_KNEE,
            RIGHT_KNEE to RIGHT_ANKLE,
            RIGHT_ANKLE to RIGHT_HEEL,
            RIGHT_HEEL to RIGHT_FOOT_INDEX
        )
    }

    val headCenter: NormalizedLandmark?
        get() = landmarks[NOSE] ?: landmarks[LEFT_EYE] ?: landmarks[RIGHT_EYE]

    val chestCenter: NormalizedLandmark?
        get() {
            val ls = landmarks[LEFT_SHOULDER]
            val rs = landmarks[RIGHT_SHOULDER]
            val lh = landmarks[LEFT_HIP]
            val rh = landmarks[RIGHT_HIP]
            return if (ls != null && rs != null) {
                NormalizedLandmark(
                    type = -1,
                    x = (ls.x + rs.x) * 0.5f,
                    y = (ls.y + rs.y) * 0.5f,
                    z = (ls.z + rs.z) * 0.5f,
                    likelihood = (ls.likelihood + rs.likelihood) * 0.5f
                )
            } else null
        }
}
