package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraKeyframe
import com.example.model.PoseFrameData
import com.example.model.TrackingTarget
import com.example.ui.theme.AccentPoseGreen
import com.example.ui.theme.AccentSegBlue
import com.example.ui.theme.ImmersiveDarkBg
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveTextPrimary

@Composable
fun VideoViewport(
    rawFrame: Bitmap?,
    processedFrame: Bitmap?,
    currentPose: PoseFrameData?,
    currentKeyframe: CameraKeyframe,
    trackingTarget: TrackingTarget,
    showTrackingOverlay: Boolean,
    isBeforePressed: Boolean,
    isSplitComparison: Boolean,
    splitPosition: Float,
    isMaskActive: Boolean,
    onVideoTapped: (normX: Float, normY: Float) -> Unit,
    onBeforePressedChange: (Boolean) -> Unit,
    onToggleSplitComparison: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normX = (offset.x / size.width).coerceIn(0f, 1f)
                    val normY = (offset.y / size.height).coerceIn(0f, 1f)
                    onVideoTapped(normX, normY)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Render Video Frame (Processed or Raw based on "Before" state)
        val activeBitmap = if (isBeforePressed) rawFrame else (processedFrame ?: rawFrame)

        if (activeBitmap != null) {
            Image(
                bitmap = activeBitmap.asImageBitmap(),
                contentDescription = "Video Frame",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading Frame...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        // 2. Body Tracking Skeleton & Bounding Box Overlay
        if (showTrackingOverlay && currentPose != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // Draw Bones Connections
                for ((p1Idx, p2Idx) in PoseFrameData.SKELETON_CONNECTIONS) {
                    val p1 = currentPose.landmarks[p1Idx]
                    val p2 = currentPose.landmarks[p2Idx]
                    if (p1 != null && p2 != null && p1.likelihood > 0.4f && p2.likelihood > 0.4f) {
                        drawLine(
                            color = ImmersivePrimary.copy(alpha = 0.85f),
                            start = Offset(p1.x * canvasW, p1.y * canvasH),
                            end = Offset(p2.x * canvasW, p2.y * canvasH),
                            strokeWidth = 5f
                        )
                    }
                }

                // Draw Landmark Joint Dots
                for (landmark in currentPose.landmarks.values) {
                    if (landmark.likelihood > 0.4f) {
                        drawCircle(
                            color = AccentPoseGreen,
                            radius = 6f,
                            center = Offset(landmark.x * canvasW, landmark.y * canvasH)
                        )
                    }
                }

                // Draw Target Bounding Box
                val bbox = currentPose.boundingBox
                val boxLeft = bbox.left * canvasW
                val boxTop = bbox.top * canvasH
                val boxW = bbox.width() * canvasW
                val boxH = bbox.height() * canvasH

                drawRoundRect(
                    color = ImmersivePrimary.copy(alpha = 0.9f),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxW, boxH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                    style = Stroke(width = 4f)
                )

                // Motion Trajectory Path
                if (trackingTarget.trajectoryPath.size > 1) {
                    for (i in 0 until trackingTarget.trajectoryPath.size - 1) {
                        val pt1 = trackingTarget.trajectoryPath[i]
                        val pt2 = trackingTarget.trajectoryPath[i + 1]
                        val alpha = ((i + 1).toFloat() / trackingTarget.trajectoryPath.size) * 0.7f
                        drawLine(
                            color = ImmersivePrimary.copy(alpha = alpha),
                            start = Offset(pt1.x * canvasW, pt1.y * canvasH),
                            end = Offset(pt2.x * canvasW, pt2.y * canvasH),
                            strokeWidth = 3f
                        )
                    }
                }
            }

            // "Target Locked" Badge above bounding box
            val bbox = currentPose.boundingBox
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (bbox.left * 320).dp,
                        y = ((bbox.top * 220).coerceAtLeast(0.05f) * 220).dp
                    )
            ) {
                Surface(
                    color = ImmersivePrimary,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = if (trackingTarget.isLocked) "TARGET LOCKED" else "AUTO DETECT",
                        color = ImmersiveOnPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 3. AI Status Card HUD (Pose Confidence & Mask status)
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Pose Confidence HUD
                val confPercent = if (currentPose != null) (currentPose.overallConfidence * 100).toInt() else 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentPoseGreen.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "  Pose Conf: $confPercent%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // AI Segmentation HUD
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isMaskActive) AccentSegBlue.copy(alpha = pulseAlpha) else Color.Gray)
                    )
                    Text(
                        text = if (isMaskActive) "  Seg Mask: Active" else "  Seg Mask: Standby",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. Quick "Before" Press-and-Hold Button
        Surface(
            color = if (isBeforePressed) ImmersivePrimary else Color.Black.copy(alpha = 0.60f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onBeforePressedChange(true)
                            tryAwaitRelease()
                            onBeforePressedChange(false)
                        }
                    )
                }
                .testTag("before_after_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = if (isBeforePressed) ImmersiveOnPrimary else Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isBeforePressed) " ORIGINAL" else " HOLD: BEFORE",
                    color = if (isBeforePressed) ImmersiveOnPrimary else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
