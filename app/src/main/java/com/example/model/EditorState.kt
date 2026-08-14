package com.example.model

enum class EditorTab(val label: String, val iconName: String) {
    CAMERA("Camera", "videocam"),
    TRACK("Tracking", "accessibility"),
    FILTERS("Filters", "palette"),
    COLOR("Color", "tune"),
    BACKGROUND("BG AI", "layers"),
    HSL("HSL", "color_lens")
}

data class ProcessProgress(
    val stageTitle: String = "",
    val progressPercent: Int = 0,
    val details: String = "",
    val isCancelable: Boolean = true
)

data class UndoSnapshot(
    val cameraConfig: CameraMotionConfig,
    val cameraKeyframes: List<CameraKeyframe>,
    val filterSettings: FilterSettings,
    val colorAdjustments: ColorAdjustments,
    val backgroundEnhancement: BackgroundEnhancement,
    val hslAdjustments: HslAdjustments,
    val trimStartMs: Long,
    val trimEndMs: Long
)
