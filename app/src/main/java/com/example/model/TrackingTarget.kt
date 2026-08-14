package com.example.model

import android.graphics.PointF
import android.graphics.RectF

data class TrackingTarget(
    val targetId: Int = 1,
    val isLocked: Boolean = true,
    val initialSelectionPoint: PointF? = null,
    val trajectoryPath: List<PointF> = emptyList(),
    val isTrackingActive: Boolean = true,
    val occlusionCounter: Int = 0,
    val targetLabel: String = "Person 1"
)

enum class TargetSelectionMode {
    AUTO_SELECT,
    TAP_TO_SELECT,
    TARGET_LOCK
}
