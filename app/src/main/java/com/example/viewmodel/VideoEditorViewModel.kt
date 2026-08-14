package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.BackgroundSegmenter
import com.example.ai.BodyPoseDetector
import com.example.ai.TargetTracker
import com.example.ai.VirtualCameraController
import com.example.model.BackgroundEnhancement
import com.example.model.BackgroundPreset
import com.example.model.CameraKeyframe
import com.example.model.CameraMotionConfig
import com.example.model.ColorAdjustments
import com.example.model.EditorTab
import com.example.model.ExportConfig
import com.example.model.FilterSettings
import com.example.model.FilterType
import com.example.model.HslAdjustments
import com.example.model.HslColorChannel
import com.example.model.PoseFrameData
import com.example.model.ProcessProgress
import com.example.model.TargetSelectionMode
import com.example.model.TrackingTarget
import com.example.model.UndoSnapshot
import com.example.model.VideoMetadata
import com.example.processing.ImageProcessingPipeline
import com.example.processing.SampleVideoGenerator
import com.example.processing.VideoExportEngine
import com.example.processing.VideoFrameExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val frameExtractor = VideoFrameExtractor(context)
    private val poseDetector = BodyPoseDetector(context)
    private val targetTracker = TargetTracker()
    private val backgroundSegmenter = BackgroundSegmenter()
    private val cameraController = VirtualCameraController()
    private val exportEngine = VideoExportEngine(context)

    // Current State
    val videoMetadata = MutableStateFlow<VideoMetadata?>(null)
    val activeTab = MutableStateFlow(EditorTab.CAMERA)
    val playheadMs = MutableStateFlow(0L)
    val isPlaying = MutableStateFlow(false)
    val trimStartMs = MutableStateFlow(0L)
    val trimEndMs = MutableStateFlow(0L)

    // AI Tracking State
    val poseMap = MutableStateFlow<Map<Long, PoseFrameData>>(emptyMap())
    val currentPose = MutableStateFlow<PoseFrameData?>(null)
    val trackingTarget = MutableStateFlow(TrackingTarget())
    val targetSelectionMode = MutableStateFlow(TargetSelectionMode.AUTO_SELECT)
    val showTrackingOverlay = MutableStateFlow(true)
    val isAnalyzingPose = MutableStateFlow(false)
    val poseAnalysisProgress = MutableStateFlow(0)

    // Virtual Camera & Keyframes
    val cameraConfig = MutableStateFlow(CameraMotionConfig())
    val cameraKeyframes = MutableStateFlow<List<CameraKeyframe>>(emptyList())
    val currentKeyframe = MutableStateFlow(CameraKeyframe(0L, 0.5f, 0.5f, 1.0f))

    // Filters, Color, AI Background, HSL
    val filterSettings = MutableStateFlow(FilterSettings())
    val colorAdjustments = MutableStateFlow(ColorAdjustments())
    val backgroundEnhancement = MutableStateFlow(BackgroundEnhancement())
    val hslAdjustments = MutableStateFlow(HslAdjustments())
    val selectedHslChannel = MutableStateFlow(HslColorChannel.RED)

    // Preview
    val rawPreviewFrame = MutableStateFlow<Bitmap?>(null)
    val processedPreviewFrame = MutableStateFlow<Bitmap?>(null)
    val currentMask = MutableStateFlow<Bitmap?>(null)
    val isBeforePressed = MutableStateFlow(false)
    val isSplitComparison = MutableStateFlow(false)
    val splitPosition = MutableStateFlow(0.5f)

    // Export & Dialogs
    val exportConfig = MutableStateFlow(ExportConfig())
    val isExporting = MutableStateFlow(false)
    val exportProgress = MutableStateFlow(ProcessProgress())
    val exportedVideoUri = MutableStateFlow<Uri?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<UndoSnapshot>()
    private val redoStack = mutableListOf<UndoSnapshot>()
    val canUndo = MutableStateFlow(false)
    val canRedo = MutableStateFlow(false)

    private var poseAnalysisJob: Job? = null
    private var renderJob: Job? = null

    fun loadVideo(uri: Uri, isSample: Boolean = false) {
        viewModelScope.launch {
            try {
                val meta = frameExtractor.extractMetadata(uri).copy(isDemoSample = isSample)
                videoMetadata.value = meta
                playheadMs.value = 0L
                trimStartMs.value = 0L
                trimEndMs.value = meta.durationMs
                isPlaying.value = false

                // Reset adjustments
                cameraConfig.value = CameraMotionConfig()
                cameraKeyframes.value = emptyList()
                filterSettings.value = FilterSettings()
                colorAdjustments.value = ColorAdjustments()
                backgroundEnhancement.value = BackgroundEnhancement()
                hslAdjustments.value = HslAdjustments()
                targetTracker.reset()
                trackingTarget.value = targetTracker.getTarget()

                undoStack.clear()
                redoStack.clear()
                updateUndoRedoStatus()

                // Fetch initial preview frame and trigger AI analysis
                refreshCurrentFrame()
                analyzeVideoBodyPose()
            } catch (e: Exception) {
                errorMessage.value = "Failed to load video: ${e.localizedMessage}"
            }
        }
    }

    fun loadDemoSample(sampleType: String = "dance") {
        viewModelScope.launch {
            try {
                isAnalyzingPose.value = true
                poseAnalysisProgress.value = 10
                val sampleUri = SampleVideoGenerator.generateSampleVideo(context, sampleType) { progress ->
                    poseAnalysisProgress.value = progress / 2
                }
                isAnalyzingPose.value = false
                loadVideo(sampleUri, isSample = true)
            } catch (e: Exception) {
                isAnalyzingPose.value = false
                errorMessage.value = "Failed to create sample video: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Runs frame-by-frame ML Kit Pose Detection across the video to generate tracking dataset.
     */
    fun analyzeVideoBodyPose() {
        val meta = videoMetadata.value ?: return
        poseAnalysisJob?.cancel()
        poseAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            isAnalyzingPose.value = true
            poseAnalysisProgress.value = 0

            val duration = meta.durationMs.coerceAtLeast(1000L)
            val stepMs = 100L // 10 fps analysis for speed & high accuracy
            val totalSteps = (duration / stepMs).toInt().coerceAtLeast(1)

            val newPoseMap = mutableMapOf<Long, PoseFrameData>()
            val keyframeList = mutableListOf<CameraKeyframe>()

            cameraController.reset()

            for (step in 0..totalSteps) {
                val timestamp = (step * stepMs).coerceAtMost(duration)
                val frame = frameExtractor.getFrameAt(meta.uri, timestamp, 640, 360)
                if (frame != null) {
                    val poseData = poseDetector.detectPose(frame, timestamp)
                    frame.recycle()

                    if (poseData != null) {
                        newPoseMap[timestamp] = poseData
                        targetTracker.updateFrame(poseData)
                    }

                    // Compute virtual camera follow
                    val kf = cameraController.updateFrame(
                        pose = poseData,
                        config = cameraConfig.value,
                        dtSeconds = stepMs / 1000f
                    )
                    keyframeList.add(kf.copy(timestampMs = timestamp))
                }

                val progress = ((step.toFloat() / totalSteps) * 100).toInt()
                withContext(Dispatchers.Main) {
                    poseAnalysisProgress.value = progress
                }
            }

            withContext(Dispatchers.Main) {
                poseMap.value = newPoseMap
                cameraKeyframes.value = keyframeList
                trackingTarget.value = targetTracker.getTarget()
                isAnalyzingPose.value = false
                refreshCurrentFrame()
            }
        }
    }

    fun seekTo(timeMs: Long) {
        val meta = videoMetadata.value ?: return
        val clamped = timeMs.coerceIn(0L, meta.durationMs)
        playheadMs.value = clamped
        refreshCurrentFrame()
    }

    fun stepFrame(forward: Boolean) {
        val stepMs = 33L // ~30 fps step
        val target = if (forward) playheadMs.value + stepMs else playheadMs.value - stepMs
        seekTo(target)
    }

    fun togglePlayPause() {
        isPlaying.value = !isPlaying.value
    }

    fun refreshCurrentFrame() {
        val meta = videoMetadata.value ?: return
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.IO) {
            val ts = playheadMs.value
            // 1. Get raw frame bitmap
            val raw = frameExtractor.getFrameAt(meta.uri, ts, 960, 540) ?: return@launch

            // 2. Lookup or detect pose
            var pose = poseMap.value[ts]
            if (pose == null && !isAnalyzingPose.value) {
                pose = poseDetector.detectPose(raw, ts)
            }

            // 3. Optional segmentation mask for preview if background tab or filters active
            var mask: Bitmap? = null
            if (activeTab.value == EditorTab.BACKGROUND || backgroundEnhancement.value.bgSaturation != 1.0f || hslAdjustments.value.backgroundOnly) {
                mask = backgroundSegmenter.generateMask(raw)
            }

            // 4. Evaluate Virtual Camera Keyframe
            val keyframe = cameraController.evaluateKeyframeAt(ts, cameraKeyframes.value)

            // 5. Apply full filter & color stack
            val processed = ImageProcessingPipeline.processFrame(
                sourceBitmap = raw,
                keyframe = keyframe,
                filterSettings = filterSettings.value,
                colorAdjustments = colorAdjustments.value,
                backgroundEnhancement = backgroundEnhancement.value,
                hslAdjustments = hslAdjustments.value,
                segmentationMask = mask,
                targetWidth = 960,
                targetHeight = 540
            )

            withContext(Dispatchers.Main) {
                rawPreviewFrame.value = raw
                processedPreviewFrame.value = processed

                currentPose.value = pose
                currentKeyframe.value = keyframe
                currentMask.value = mask
            }
        }
    }

    // Camera Configuration & Keyframe adjustments
    fun updateCameraConfig(update: (CameraMotionConfig) -> CameraMotionConfig) {
        saveUndoState()
        cameraConfig.value = update(cameraConfig.value)
        recomputeCameraMovement()
    }

    fun recomputeCameraMovement() {
        val meta = videoMetadata.value ?: return
        val poses = poseMap.value
        if (poses.isEmpty()) return

        cameraController.reset()
        val duration = meta.durationMs
        val stepMs = 100L
        val totalSteps = (duration / stepMs).toInt()
        val newKeyframes = mutableListOf<CameraKeyframe>()

        for (step in 0..totalSteps) {
            val ts = (step * stepMs).coerceAtMost(duration)
            val pose = poses[ts]
            val kf = cameraController.updateFrame(pose, cameraConfig.value, stepMs / 1000f)
            newKeyframes.add(kf.copy(timestampMs = ts))
        }
        cameraKeyframes.value = newKeyframes
        refreshCurrentFrame()
    }

    fun addManualKeyframe(focusX: Float, focusY: Float, zoom: Float) {
        saveUndoState()
        val ts = playheadMs.value
        val list = cameraKeyframes.value.toMutableList()
        list.removeAll { it.timestampMs == ts }
        list.add(CameraKeyframe(ts, focusX, focusY, zoom, isAutoGenerated = false))
        list.sortBy { it.timestampMs }
        cameraKeyframes.value = list
        refreshCurrentFrame()
    }

    fun deleteCurrentKeyframe() {
        val ts = playheadMs.value
        val list = cameraKeyframes.value.toMutableList()
        val removed = list.removeAll { kotlin.math.abs(it.timestampMs - ts) < 150L }
        if (removed) {
            saveUndoState()
            cameraKeyframes.value = list
            refreshCurrentFrame()
        }
    }

    fun jumpToPrevKeyframe() {
        val ts = playheadMs.value
        val prev = cameraKeyframes.value.lastOrNull { it.timestampMs < ts - 100L }
        if (prev != null) {
            seekTo(prev.timestampMs)
        }
    }

    fun jumpToNextKeyframe() {
        val ts = playheadMs.value
        val next = cameraKeyframes.value.firstOrNull { it.timestampMs > ts + 100L }
        if (next != null) {
            seekTo(next.timestampMs)
        }
    }

    fun resetCameraMovement() {
        saveUndoState()
        cameraConfig.value = CameraMotionConfig()
        recomputeCameraMovement()
    }

    // Target Selection
    fun onVideoTapped(normX: Float, normY: Float) {
        targetTracker.selectTargetByTap(normX, normY, currentPose.value)
        trackingTarget.value = targetTracker.getTarget()
        recomputeCameraMovement()
    }

    fun setTargetLock(locked: Boolean) {
        targetTracker.setLock(locked)
        trackingTarget.value = targetTracker.getTarget()
    }

    fun autoSelectTarget() {
        targetTracker.autoSelectTarget(currentPose.value)
        trackingTarget.value = targetTracker.getTarget()
        recomputeCameraMovement()
    }

    // Filters & Grading
    fun setFilter(filterType: FilterType, intensity: Float = 1.0f) {
        saveUndoState()
        filterSettings.value = FilterSettings(filterType, intensity)
        refreshCurrentFrame()
    }

    fun setFilterIntensity(intensity: Float) {
        filterSettings.value = filterSettings.value.copy(intensity = intensity)
        refreshCurrentFrame()
    }

    fun updateColor(update: (ColorAdjustments) -> ColorAdjustments) {
        colorAdjustments.value = update(colorAdjustments.value)
        refreshCurrentFrame()
    }

    fun resetColor() {
        saveUndoState()
        colorAdjustments.value = ColorAdjustments.DEFAULT
        refreshCurrentFrame()
    }

    // Background AI
    fun updateBackground(update: (BackgroundEnhancement) -> BackgroundEnhancement) {
        backgroundEnhancement.value = update(backgroundEnhancement.value)
        refreshCurrentFrame()
    }

    fun setBackgroundPreset(preset: BackgroundPreset) {
        saveUndoState()
        backgroundEnhancement.value = BackgroundEnhancement.fromPreset(
            preset,
            backgroundEnhancement.value.protectSubject,
            backgroundEnhancement.value.protectionStrength
        )
        refreshCurrentFrame()
    }

    // HSL
    fun updateHsl(update: (HslAdjustments) -> HslAdjustments) {
        hslAdjustments.value = update(hslAdjustments.value)
        refreshCurrentFrame()
    }

    // Undo / Redo
    private fun saveUndoState() {
        val snapshot = UndoSnapshot(
            cameraConfig = cameraConfig.value,
            cameraKeyframes = cameraKeyframes.value,
            filterSettings = filterSettings.value,
            colorAdjustments = colorAdjustments.value,
            backgroundEnhancement = backgroundEnhancement.value,
            hslAdjustments = hslAdjustments.value,
            trimStartMs = trimStartMs.value,
            trimEndMs = trimEndMs.value
        )
        undoStack.add(snapshot)
        if (undoStack.size > 20) undoStack.removeAt(0)
        redoStack.clear()
        updateUndoRedoStatus()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentSnapshot = UndoSnapshot(
            cameraConfig = cameraConfig.value,
            cameraKeyframes = cameraKeyframes.value,
            filterSettings = filterSettings.value,
            colorAdjustments = colorAdjustments.value,
            backgroundEnhancement = backgroundEnhancement.value,
            hslAdjustments = hslAdjustments.value,
            trimStartMs = trimStartMs.value,
            trimEndMs = trimEndMs.value
        )
        redoStack.add(currentSnapshot)

        val previous = undoStack.removeAt(undoStack.size - 1)
        applySnapshot(previous)
        updateUndoRedoStatus()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentSnapshot = UndoSnapshot(
            cameraConfig = cameraConfig.value,
            cameraKeyframes = cameraKeyframes.value,
            filterSettings = filterSettings.value,
            colorAdjustments = colorAdjustments.value,
            backgroundEnhancement = backgroundEnhancement.value,
            hslAdjustments = hslAdjustments.value,
            trimStartMs = trimStartMs.value,
            trimEndMs = trimEndMs.value
        )
        undoStack.add(currentSnapshot)

        val next = redoStack.removeAt(redoStack.size - 1)
        applySnapshot(next)
        updateUndoRedoStatus()
    }

    private fun applySnapshot(s: UndoSnapshot) {
        cameraConfig.value = s.cameraConfig
        cameraKeyframes.value = s.cameraKeyframes
        filterSettings.value = s.filterSettings
        colorAdjustments.value = s.colorAdjustments
        backgroundEnhancement.value = s.backgroundEnhancement
        hslAdjustments.value = s.hslAdjustments
        trimStartMs.value = s.trimStartMs
        trimEndMs.value = s.trimEndMs
        refreshCurrentFrame()
    }

    private fun updateUndoRedoStatus() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    // Export
    fun startExport() {
        val meta = videoMetadata.value ?: return
        isExporting.value = true
        exportedVideoUri.value = null
        exportProgress.value = ProcessProgress("Preparing Export", 0, "Configuring encoder...")

        viewModelScope.launch {
            val uri = exportEngine.exportVideo(
                sourceMetadata = meta,
                keyframes = cameraKeyframes.value,
                filterSettings = filterSettings.value,
                colorAdjustments = colorAdjustments.value,
                backgroundEnhancement = backgroundEnhancement.value,
                hslAdjustments = hslAdjustments.value,
                poseMap = poseMap.value,
                exportConfig = exportConfig.value,
                trimStartMs = trimStartMs.value,
                trimEndMs = trimEndMs.value
            ) { stage, percent, detail ->
                exportProgress.value = ProcessProgress(stage, percent, detail)
            }

            if (uri != null) {
                exportedVideoUri.value = uri
            } else {
                errorMessage.value = "Export was cancelled or failed."
                isExporting.value = false
            }
        }
    }

    fun cancelExport() {
        exportEngine.cancel()
        isExporting.value = false
    }

    fun dismissExportDialog() {
        isExporting.value = false
        exportedVideoUri.value = null
    }

    override fun onCleared() {
        super.onCleared()
        poseDetector.close()
        backgroundSegmenter.close()
    }
}
