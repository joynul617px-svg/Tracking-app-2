package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.EditorTab
import com.example.ui.components.BackgroundAITab
import com.example.ui.components.CameraControlsView
import com.example.ui.components.ColorTab
import com.example.ui.components.ExportDialog
import com.example.ui.components.FiltersTab
import com.example.ui.components.HslTab
import com.example.ui.components.ImportView
import com.example.ui.components.TimelineScrubber
import com.example.ui.components.TopBar
import com.example.ui.components.TrackingControlsView
import com.example.ui.components.VideoViewport
import com.example.ui.theme.ImmersiveDarkBg
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceContainer
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary
import com.example.viewmodel.VideoEditorViewModel
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(
    viewModel: VideoEditorViewModel = viewModel()
) {
    val metadata by viewModel.videoMetadata.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val playheadMs by viewModel.playheadMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val rawFrame by viewModel.rawPreviewFrame.collectAsState()
    val processedFrame by viewModel.processedPreviewFrame.collectAsState()
    val currentPose by viewModel.currentPose.collectAsState()
    val currentKeyframe by viewModel.currentKeyframe.collectAsState()
    val trackingTarget by viewModel.trackingTarget.collectAsState()
    val showTrackingOverlay by viewModel.showTrackingOverlay.collectAsState()
    val isAnalyzingPose by viewModel.isAnalyzingPose.collectAsState()
    val poseProgress by viewModel.poseAnalysisProgress.collectAsState()

    val cameraConfig by viewModel.cameraConfig.collectAsState()
    val keyframes by viewModel.cameraKeyframes.collectAsState()

    val filterSettings by viewModel.filterSettings.collectAsState()
    val colorAdjustments by viewModel.colorAdjustments.collectAsState()
    val backgroundEnhancement by viewModel.backgroundEnhancement.collectAsState()
    val hslAdjustments by viewModel.hslAdjustments.collectAsState()
    val selectedHslChannel by viewModel.selectedHslChannel.collectAsState()
    val currentMask by viewModel.currentMask.collectAsState()

    val isBeforePressed by viewModel.isBeforePressed.collectAsState()
    val isSplitComparison by viewModel.isSplitComparison.collectAsState()
    val splitPosition by viewModel.splitPosition.collectAsState()

    val exportConfig by viewModel.exportConfig.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportedUri by viewModel.exportedVideoUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Playback loop
    LaunchedEffect(isPlaying, metadata) {
        if (isPlaying && metadata != null) {
            val duration = metadata!!.durationMs
            while (isPlaying) {
                delay(33L)
                val next = viewModel.playheadMs.value + 33L
                if (next >= duration) {
                    viewModel.seekTo(0L)
                } else {
                    viewModel.seekTo(next)
                }
            }
        }
    }

    // Error message display
    LaunchedEffect(errorMessage) {
        val err = errorMessage
        if (err != null) {
            snackbarHostState.showSnackbar(err)
            viewModel.errorMessage.value = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ImmersiveDarkBg
    ) { paddingValues ->
        if (metadata == null) {
            // Import screen
            ImportView(
                isAnalyzing = isAnalyzingPose,
                analysisProgress = poseProgress,
                onVideoSelected = { uri -> viewModel.loadVideo(uri) },
                onLoadSample = { sampleType -> viewModel.loadDemoSample(sampleType) },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // Main Editor Interface
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1. Top Bar
                TopBar(
                    metadata = metadata,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onBackClick = { viewModel.videoMetadata.value = null },
                    onUndoClick = { viewModel.undo() },
                    onRedoClick = { viewModel.redo() },
                    onExportClick = { showExportDialog = true }
                )

                // 2. Video Viewport (Takes remaining top space)
                VideoViewport(
                    rawFrame = rawFrame,
                    processedFrame = processedFrame,
                    currentPose = currentPose,
                    currentKeyframe = currentKeyframe,
                    trackingTarget = trackingTarget,
                    showTrackingOverlay = showTrackingOverlay,
                    isBeforePressed = isBeforePressed,
                    isSplitComparison = isSplitComparison,
                    splitPosition = splitPosition,
                    isMaskActive = currentMask != null,
                    onVideoTapped = { nx, ny -> viewModel.onVideoTapped(nx, ny) },
                    onBeforePressedChange = { viewModel.isBeforePressed.value = it },
                    onToggleSplitComparison = { viewModel.isSplitComparison.value = !viewModel.isSplitComparison.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                )

                // 3. Timeline Scrubber
                TimelineScrubber(
                    currentMs = playheadMs,
                    totalDurationMs = metadata?.durationMs ?: 1L,
                    isPlaying = isPlaying,
                    keyframes = keyframes,
                    onSeek = { ms -> viewModel.seekTo(ms) },
                    onStepPrev = { viewModel.stepFrame(forward = false) },
                    onStepNext = { viewModel.stepFrame(forward = true) },
                    onTogglePlay = { viewModel.togglePlayPause() }
                )

                // 4. Tab Content Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                        .background(ImmersiveSurfaceContainer)
                ) {
                    when (activeTab) {
                        EditorTab.CAMERA -> CameraControlsView(
                            config = cameraConfig,
                            currentKeyframe = currentKeyframe,
                            onConfigChange = { viewModel.updateCameraConfig(it) },
                            onAddKeyframe = {
                                viewModel.addManualKeyframe(
                                    currentKeyframe.focusX,
                                    currentKeyframe.focusY,
                                    currentKeyframe.zoom
                                )
                            },
                            onDeleteKeyframe = { viewModel.deleteCurrentKeyframe() },
                            onPrevKeyframe = { viewModel.jumpToPrevKeyframe() },
                            onNextKeyframe = { viewModel.jumpToNextKeyframe() },
                            onResetCamera = { viewModel.resetCameraMovement() }
                        )

                        EditorTab.TRACK -> TrackingControlsView(
                            currentPose = currentPose,
                            trackingTarget = trackingTarget,
                            showTrackingOverlay = showTrackingOverlay,
                            isAnalyzing = isAnalyzingPose,
                            analysisProgress = poseProgress,
                            onToggleShowOverlay = { viewModel.showTrackingOverlay.value = it },
                            onAutoSelect = { viewModel.autoSelectTarget() },
                            onToggleLock = { viewModel.setTargetLock(it) },
                            onReAnalyze = { viewModel.analyzeVideoBodyPose() }
                        )

                        EditorTab.FILTERS -> FiltersTab(
                            settings = filterSettings,
                            onSelectFilter = { viewModel.setFilter(it) },
                            onIntensityChange = { viewModel.setFilterIntensity(it) }
                        )

                        EditorTab.COLOR -> ColorTab(
                            adjustments = colorAdjustments,
                            onAdjustmentsChange = { viewModel.updateColor(it) },
                            onResetColor = { viewModel.resetColor() }
                        )

                        EditorTab.BACKGROUND -> BackgroundAITab(
                            enhancement = backgroundEnhancement,
                            onEnhancementChange = { viewModel.updateBackground(it) },
                            onSelectPreset = { viewModel.setBackgroundPreset(it) }
                        )

                        EditorTab.HSL -> HslTab(
                            adjustments = hslAdjustments,
                            selectedChannel = selectedHslChannel,
                            onSelectChannel = { viewModel.selectedHslChannel.value = it },
                            onAdjustmentsChange = { viewModel.updateHsl(it) }
                        )
                    }
                }

                // 5. Bottom Navigation Bar
                NavigationBar(
                    containerColor = ImmersiveSurface,
                    contentColor = ImmersiveTextPrimary,
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    val tabs = listOf(
                        EditorTab.CAMERA to Icons.Default.Videocam,
                        EditorTab.TRACK to Icons.Default.Accessibility,
                        EditorTab.FILTERS to Icons.Default.Palette,
                        EditorTab.COLOR to Icons.Default.Tune,
                        EditorTab.BACKGROUND to Icons.Default.Layers,
                        EditorTab.HSL to Icons.Default.ColorLens
                    )

                    tabs.forEach { (tab, icon) ->
                        val isSelected = activeTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                viewModel.activeTab.value = tab
                                viewModel.refreshCurrentFrame()
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ImmersivePrimary,
                                selectedTextColor = ImmersivePrimary,
                                unselectedIconColor = ImmersiveTextSecondary,
                                unselectedTextColor = ImmersiveTextSecondary,
                                indicatorColor = ImmersivePrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            // Export Dialog
            if (showExportDialog || isExporting || exportedUri != null) {
                ExportDialog(
                    metadata = metadata,
                    config = exportConfig,
                    isExporting = isExporting,
                    progress = exportProgress,
                    exportedUri = exportedUri,
                    onConfigChange = { viewModel.exportConfig.value = it(viewModel.exportConfig.value) },
                    onStartExport = { viewModel.startExport() },
                    onCancelExport = { viewModel.cancelExport() },
                    onDismiss = {
                        showExportDialog = false
                        viewModel.dismissExportDialog()
                    }
                )
            }
        }
    }
}
