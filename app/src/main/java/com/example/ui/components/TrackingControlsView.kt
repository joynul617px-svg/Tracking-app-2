package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PoseFrameData
import com.example.model.TrackingTarget
import com.example.ui.theme.AccentPoseGreen
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurfaceContainer
import com.example.ui.theme.ImmersiveSurfaceHigh
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun TrackingControlsView(
    currentPose: PoseFrameData?,
    trackingTarget: TrackingTarget,
    showTrackingOverlay: Boolean,
    isAnalyzing: Boolean,
    analysisProgress: Int,
    onToggleShowOverlay: (Boolean) -> Unit,
    onAutoSelect: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onReAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Analysis Progress bar if active
        if (isAnalyzing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceHigh),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Analyzing Body Pose...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersivePrimary
                        )
                        Text(
                            text = "$analysisProgress%",
                            fontSize = 12.sp,
                            color = ImmersiveTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { analysisProgress / 100f },
                        color = ImmersivePrimary,
                        trackColor = ImmersiveSurfaceContainer,
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }
            }
        }

        // Show Tracking Overlay Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Tracking Skeleton",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersiveTextPrimary
                    )
                    Text(
                        text = "Display skeleton bones, landmark joints & bounding box",
                        fontSize = 10.sp,
                        color = ImmersiveTextSecondary
                    )
                }
                Switch(
                    checked = showTrackingOverlay,
                    onCheckedChange = onToggleShowOverlay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ImmersivePrimary,
                        checkedTrackColor = ImmersivePrimary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("show_tracking_switch")
                )
            }
        }

        // Target Person Selection Options
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TARGET PERSON SELECTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersivePrimary,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAutoSelect,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("auto_select_target_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = ImmersiveOnPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Auto Select",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveOnPrimary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Button(
                        onClick = { onToggleLock(!trackingTarget.isLocked) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (trackingTarget.isLocked) ImmersivePrimary else ImmersiveSurfaceHigh
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("target_lock_btn")
                    ) {
                        Icon(
                            imageVector = if (trackingTarget.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (trackingTarget.isLocked) ImmersiveOnPrimary else ImmersiveTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (trackingTarget.isLocked) "Locked" else "Unlock",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (trackingTarget.isLocked) ImmersiveOnPrimary else ImmersiveTextPrimary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = ImmersivePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " Tap directly on any person in the video viewport to lock tracking onto them.",
                        fontSize = 11.sp,
                        color = ImmersiveTextSecondary
                    )
                }
            }
        }

        // Tracking Status & Occlusion Recovery Stats
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "AI TRACKING TELEMETRY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersivePrimary,
                    letterSpacing = 0.8.sp
                )

                val conf = if (currentPose != null) (currentPose.overallConfidence * 100).toInt() else 0
                Text(
                    text = "• Overall Body Confidence: $conf%",
                    fontSize = 11.sp,
                    color = ImmersiveTextPrimary
                )
                Text(
                    text = "• 33 Key Body Landmarks: ${if (currentPose != null) "Detected (Nose, Chest, Spine, Knees, Ankles, Feet)" else "Searching..."}",
                    fontSize = 11.sp,
                    color = ImmersiveTextSecondary
                )
                Text(
                    text = "• Occlusion Recovery: Active (maintains trajectory across temporary obstacles)",
                    fontSize = 11.sp,
                    color = ImmersiveTextSecondary
                )
            }
        }

        // Re-analyze Button
        OutlinedButton(
            onClick = onReAnalyze,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersivePrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("reanalyze_pose_btn")
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
            Text("Re-Analyze Full Video Body Pose", fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
}
