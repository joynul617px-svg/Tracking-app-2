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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterSettings
import com.example.model.FilterType
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurfaceContainer
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun FiltersTab(
    settings: FilterSettings,
    onSelectFilter: (FilterType) -> Unit,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Filter Intensity Slider (visible if not NONE)
        if (settings.activeFilter != FilterType.NONE) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FILTER INTENSITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "${(settings.intensity * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ImmersiveTextPrimary
                    )
                }
                Slider(
                    value = settings.intensity,
                    onValueChange = onIntensityChange,
                    colors = SliderDefaults.colors(
                        thumbColor = ImmersivePrimary,
                        activeTrackColor = ImmersivePrimary,
                        inactiveTrackColor = ImmersiveOutline
                    ),
                    modifier = Modifier.testTag("filter_intensity_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal List of 16 Filter Presets
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(FilterType.entries.toTypedArray()) { filterType ->
                val isSelected = settings.activeFilter == filterType
                FilterPresetCard(
                    filterType = filterType,
                    isSelected = isSelected,
                    onClick = { onSelectFilter(filterType) }
                )
            }
        }
    }
}

@Composable
fun FilterPresetCard(
    filterType: FilterType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .testTag("filter_${filterType.name.lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(filterType.previewGradientStart, filterType.previewGradientEnd)
                    )
                )
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, ImmersivePrimary, RoundedCornerShape(14.dp))
                    } else {
                        Modifier.border(1.dp, ImmersiveOutline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = filterType.displayName.take(3).uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = filterType.displayName,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) ImmersivePrimary else ImmersiveTextSecondary,
            maxLines = 1
        )
    }
}
