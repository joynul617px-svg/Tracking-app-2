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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.HslAdjustments
import com.example.model.HslChannelAdjustment
import com.example.model.HslColorChannel
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurfaceContainer
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun HslTab(
    adjustments: HslAdjustments,
    selectedChannel: HslColorChannel,
    onSelectChannel: (HslColorChannel) -> Unit,
    onAdjustmentsChange: ((HslAdjustments) -> HslAdjustments) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentChannelAdjustment = adjustments.channels[selectedChannel] ?: HslChannelAdjustment()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // "Background Only" toggle card
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
                        text = "Background Only",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersiveTextPrimary
                    )
                    Text(
                        text = "Apply selective HSL adjustments only to background mask",
                        fontSize = 10.sp,
                        color = ImmersiveTextSecondary
                    )
                }
                Switch(
                    checked = adjustments.backgroundOnly,
                    onCheckedChange = { checked ->
                        onAdjustmentsChange { it.copy(backgroundOnly = checked) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ImmersivePrimary,
                        checkedTrackColor = ImmersivePrimary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("hsl_bg_only_switch")
                )
            }
        }

        // Color Channel Dots Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items(HslColorChannel.entries.toTypedArray()) { channel ->
                val isSelected = selectedChannel == channel
                val hasModification = adjustments.channels[channel]?.let {
                    it.hue != 0f || it.saturation != 0f || it.luminance != 0f
                } == true

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectChannel(channel) }
                        .padding(horizontal = 4.dp)
                        .testTag("hsl_channel_${channel.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(channel.baseColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, Color.White, CircleShape)
                                } else if (hasModification) {
                                    Modifier.border(1.5.dp, ImmersivePrimary, CircleShape)
                                } else {
                                    Modifier.border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasModification) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = channel.displayName,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) ImmersivePrimary else ImmersiveTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Selected Channel Controls
        Text(
            text = "${selectedChannel.displayName.uppercase()} CHANNEL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = selectedChannel.baseColor,
            letterSpacing = 0.8.sp
        )

        // Hue
        ControlSlider(
            label = "Hue",
            value = (currentChannelAdjustment.hue + 100f) / 200f,
            valueDisplay = String.format("%+.0f°", currentChannelAdjustment.hue),
            onValueChange = { v ->
                val hueVal = v * 200f - 100f
                onAdjustmentsChange {
                    it.updateChannel(selectedChannel) { curr -> curr.copy(hue = hueVal) }
                }
            },
            testTag = "hsl_hue_slider"
        )

        // Saturation
        ControlSlider(
            label = "Saturation",
            value = (currentChannelAdjustment.saturation + 100f) / 200f,
            valueDisplay = String.format("%+.0f%%", currentChannelAdjustment.saturation),
            onValueChange = { v ->
                val satVal = v * 200f - 100f
                onAdjustmentsChange {
                    it.updateChannel(selectedChannel) { curr -> curr.copy(saturation = satVal) }
                }
            },
            testTag = "hsl_sat_slider"
        )

        // Luminance
        ControlSlider(
            label = "Luminance",
            value = (currentChannelAdjustment.luminance + 100f) / 200f,
            valueDisplay = String.format("%+.0f%%", currentChannelAdjustment.luminance),
            onValueChange = { v ->
                val lumVal = v * 200f - 100f
                onAdjustmentsChange {
                    it.updateChannel(selectedChannel) { curr -> curr.copy(luminance = lumVal) }
                }
            },
            testTag = "hsl_lum_slider"
        )
    }
}
