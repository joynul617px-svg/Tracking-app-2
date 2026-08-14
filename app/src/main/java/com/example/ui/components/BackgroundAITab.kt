package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundEnhancement
import com.example.model.BackgroundPreset
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurfaceContainer
import com.example.ui.theme.ImmersiveSurfaceHigh
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun BackgroundAITab(
    enhancement: BackgroundEnhancement,
    onEnhancementChange: ((BackgroundEnhancement) -> BackgroundEnhancement) -> Unit,
    onSelectPreset: (BackgroundPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Subject Protection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = " Protect Subject",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextPrimary
                        )
                    }
                    Switch(
                        checked = enhancement.protectSubject,
                        onCheckedChange = { checked ->
                            onEnhancementChange { it.copy(protectSubject = checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ImmersivePrimary,
                            checkedTrackColor = ImmersivePrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("protect_subject_switch")
                    )
                }

                Text(
                    text = "AI segmentation protects face, skin, hair, and clothing from harsh saturation while boosting outdoor scenery.",
                    fontSize = 10.sp,
                    color = ImmersiveTextSecondary
                )

                if (enhancement.protectSubject) {
                    ControlSlider(
                        label = "Protection Strength",
                        value = enhancement.protectionStrength,
                        valueDisplay = "${(enhancement.protectionStrength * 100).toInt()}%",
                        onValueChange = { v -> onEnhancementChange { it.copy(protectionStrength = v) } },
                        testTag = "protection_strength_slider"
                    )
                }
            }
        }

        // 2. Quick Background Presets
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "QUICK BACKGROUND PRESETS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersivePrimary,
                letterSpacing = 0.8.sp
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BackgroundPreset.entries.toTypedArray()) { preset ->
                    val isSelected = enhancement.activePreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ImmersivePrimary.copy(alpha = 0.25f) else ImmersiveSurfaceHigh)
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, ImmersivePrimary, RoundedCornerShape(10.dp))
                                else Modifier.border(1.dp, ImmersiveOutline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            )
                            .clickable { onSelectPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("bg_preset_${preset.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = preset.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ImmersivePrimary else ImmersiveTextPrimary
                            )
                            Text(
                                text = preset.description,
                                fontSize = 8.sp,
                                color = ImmersiveTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. Detailed Background Sliders
        ControlSlider(
            label = "Background Saturation",
            value = enhancement.bgSaturation / 2f,
            valueDisplay = "${(enhancement.bgSaturation * 100).toInt()}%",
            onValueChange = { v -> onEnhancementChange { it.copy(bgSaturation = v * 2f) } },
            testTag = "slider_bg_saturation"
        )

        ControlSlider(
            label = "Background Vibrance",
            value = (enhancement.bgVibrance + 1f) / 2f,
            valueDisplay = String.format("%+.0f", enhancement.bgVibrance * 100),
            onValueChange = { v -> onEnhancementChange { it.copy(bgVibrance = v * 2f - 1f) } },
            testTag = "slider_bg_vibrance"
        )

        ControlSlider(
            label = "Background Brightness",
            value = (enhancement.bgBrightness + 1f) / 2f,
            valueDisplay = String.format("%+.0f", enhancement.bgBrightness * 100),
            onValueChange = { v -> onEnhancementChange { it.copy(bgBrightness = v * 2f - 1f) } },
            testTag = "slider_bg_brightness"
        )

        ControlSlider(
            label = "Background Contrast",
            value = (enhancement.bgContrast - 0.2f) / 1.8f,
            valueDisplay = String.format("%.2f", enhancement.bgContrast),
            onValueChange = { v -> onEnhancementChange { it.copy(bgContrast = 0.2f + v * 1.8f) } },
            testTag = "slider_bg_contrast"
        )

        ControlSlider(
            label = "Background Warmth",
            value = (enhancement.bgWarmth + 1f) / 2f,
            valueDisplay = String.format("%+.0f", enhancement.bgWarmth * 100),
            onValueChange = { v -> onEnhancementChange { it.copy(bgWarmth = v * 2f - 1f) } },
            testTag = "slider_bg_warmth"
        )

        ControlSlider(
            label = "Background Sharpness",
            value = enhancement.bgSharpness,
            valueDisplay = "${(enhancement.bgSharpness * 100).toInt()}%",
            onValueChange = { v -> onEnhancementChange { it.copy(bgSharpness = v) } },
            testTag = "slider_bg_sharpness"
        )
    }
}
