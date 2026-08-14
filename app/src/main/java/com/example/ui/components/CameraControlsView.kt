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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.model.CameraKeyframe
import com.example.model.CameraMotionConfig
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceContainer
import com.example.ui.theme.ImmersiveSurfaceHigh
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun CameraControlsView(
    config: CameraMotionConfig,
    currentKeyframe: CameraKeyframe,
    onConfigChange: ((CameraMotionConfig) -> CameraMotionConfig) -> Unit,
    onAddKeyframe: () -> Unit,
    onDeleteKeyframe: () -> Unit,
    onPrevKeyframe: () -> Unit,
    onNextKeyframe: () -> Unit,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Toggles Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Auto Follow Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Auto Follow",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersiveTextPrimary
                    )
                    Switch(
                        checked = config.autoFollow,
                        onCheckedChange = { checked ->
                            onConfigChange { it.copy(autoFollow = checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ImmersivePrimary,
                            checkedTrackColor = ImmersivePrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("auto_follow_switch")
                    )
                }
            }

            // Auto Zoom Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).padding(start = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Auto Zoom",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersiveTextPrimary
                    )
                    Switch(
                        checked = config.autoZoom,
                        onCheckedChange = { checked ->
                            onConfigChange { it.copy(autoZoom = checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ImmersivePrimary,
                            checkedTrackColor = ImmersivePrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("auto_zoom_switch")
                    )
                }
            }
        }

        // Sliders
        ControlSlider(
            label = "Camera Smoothness",
            value = config.smoothness,
            valueDisplay = "${(config.smoothness * 100).toInt()}%",
            onValueChange = { v -> onConfigChange { it.copy(smoothness = v) } },
            testTag = "smoothness_slider"
        )

        ControlSlider(
            label = "Tracking Sensitivity",
            value = config.trackingSensitivity,
            valueDisplay = "${(config.trackingSensitivity * 100).toInt()}%",
            onValueChange = { v -> onConfigChange { it.copy(trackingSensitivity = v) } },
            testTag = "sensitivity_slider"
        )

        ControlSlider(
            label = "Dead Zone Threshold",
            value = config.deadZone,
            valueDisplay = "${(config.deadZone * 100).toInt()}%",
            onValueChange = { v -> onConfigChange { it.copy(deadZone = v) } },
            testTag = "dead_zone_slider"
        )

        ControlSlider(
            label = "Maximum Zoom",
            value = (config.maxZoom - 1.0f) / 9.0f,
            valueDisplay = String.format("%.1fx", config.maxZoom),
            onValueChange = { v ->
                val zoomVal = 1.0f + v * 9.0f
                onConfigChange { it.copy(maxZoom = zoomVal) }
            },
            testTag = "max_zoom_slider"
        )

        ControlSlider(
            label = "Follow Strength",
            value = config.followStrength,
            valueDisplay = "${(config.followStrength * 100).toInt()}%",
            onValueChange = { v -> onConfigChange { it.copy(followStrength = v) } },
            testTag = "follow_strength_slider"
        )

        // Axis Follow Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Horizontal",
                    fontSize = 11.sp,
                    color = ImmersiveTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = config.horizontalFollow,
                    onCheckedChange = { checked ->
                        onConfigChange { it.copy(horizontalFollow = checked) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ImmersivePrimary,
                        checkedTrackColor = ImmersivePrimary.copy(alpha = 0.3f)
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Vertical",
                    fontSize = 11.sp,
                    color = ImmersiveTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = config.verticalFollow,
                    onCheckedChange = { checked ->
                        onConfigChange { it.copy(verticalFollow = checked) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ImmersivePrimary,
                        checkedTrackColor = ImmersivePrimary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Manual Keyframe Tools
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANUAL KEYFRAMES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Zoom: ${String.format("%.1fx", currentKeyframe.zoom)}",
                        fontSize = 11.sp,
                        color = ImmersiveTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevKeyframe, modifier = Modifier.testTag("prev_kf_btn")) {
                        Icon(Icons.Default.NavigateBefore, "Prev Keyframe", tint = ImmersiveTextPrimary)
                    }

                    Button(
                        onClick = onAddKeyframe,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveSurfaceHigh),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_kf_btn")
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Text("Add KF", fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                    }

                    Button(
                        onClick = onDeleteKeyframe,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveSurfaceHigh),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("del_kf_btn")
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Text("Delete", fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                    }

                    IconButton(onClick = onNextKeyframe, modifier = Modifier.testTag("next_kf_btn")) {
                        Icon(Icons.Default.NavigateNext, "Next Keyframe", tint = ImmersiveTextPrimary)
                    }
                }
            }
        }

        // Reset Button
        OutlinedButton(
            onClick = onResetCamera,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersivePrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("reset_camera_btn")
        ) {
            Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
            Text("Reset Camera Movement", fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
fun ControlSlider(
    label: String,
    value: Float,
    valueDisplay: String,
    onValueChange: (Float) -> Unit,
    testTag: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersivePrimary,
                letterSpacing = 0.6.sp
            )
            Text(
                text = valueDisplay,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ImmersiveTextPrimary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = ImmersivePrimary,
                activeTrackColor = ImmersivePrimary,
                inactiveTrackColor = ImmersiveOutline
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
