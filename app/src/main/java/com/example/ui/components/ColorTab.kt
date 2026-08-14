package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColorAdjustments
import com.example.ui.theme.ImmersivePrimary

@Composable
fun ColorTab(
    adjustments: ColorAdjustments,
    onAdjustmentsChange: ((ColorAdjustments) -> ColorAdjustments) -> Unit,
    onResetColor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Reset Button at top
        OutlinedButton(
            onClick = onResetColor,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersivePrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("reset_color_btn")
        ) {
            Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
            Text("Reset Color Adjustments", fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }

        // 1. Exposure (-1.0 to 1.0)
        ControlSlider(
            label = "Exposure",
            value = (adjustments.exposure + 1f) / 2f,
            valueDisplay = String.format("%+.2f", adjustments.exposure),
            onValueChange = { v -> onAdjustmentsChange { it.copy(exposure = v * 2f - 1f) } },
            testTag = "slider_exposure"
        )

        // 2. Brightness (-1.0 to 1.0)
        ControlSlider(
            label = "Brightness",
            value = (adjustments.brightness + 1f) / 2f,
            valueDisplay = String.format("%+.0f", adjustments.brightness * 100),
            onValueChange = { v -> onAdjustmentsChange { it.copy(brightness = v * 2f - 1f) } },
            testTag = "slider_brightness"
        )

        // 3. Contrast (0.2 to 2.0)
        ControlSlider(
            label = "Contrast",
            value = (adjustments.contrast - 0.2f) / 1.8f,
            valueDisplay = String.format("%.2f", adjustments.contrast),
            onValueChange = { v -> onAdjustmentsChange { it.copy(contrast = 0.2f + v * 1.8f) } },
            testTag = "slider_contrast"
        )

        // 4. Highlights (-1.0 to 1.0)
        ControlSlider(
            label = "Highlights",
            value = (adjustments.highlights + 1f) / 2f,
            valueDisplay = String.format("%+.0f", adjustments.highlights * 100),
            onValueChange = { v -> onAdjustmentsChange { it.copy(highlights = v * 2f - 1f) } },
            testTag = "slider_highlights"
        )

        // 5. Shadows (-1.0 to 1.0)
        ControlSlider(
            label = "Shadows",
            value = (adjustments.shadows + 1f) / 2f,
            valueDisplay = String.format("%+.0f", adjustments.shadows * 100),
            onValueChange = { v -> onAdjustmentsChange { it.copy(shadows = v * 2f - 1f) } },
            testTag = "slider_shadows"
        )

        // 6. Whites (-1.0 to 1.0)
        ControlSlider(
            label = "Whites",
            value = (adjustments.whites + 1f) / 2f,
            valueDisplay = String.format("%+.0f", adjustments.whites * 100),
            onValueChange = { v -> onAdjustmentsChange { it.copy(whites = v * 2f - 1f) } },
            testTag = "slider_whites"
        )

        // 7. Blacks (-1.0 to 1.0)
        ControlSlider(
            label = "Blacks",
            value = (adjustments.blacks + 1f) / 2f,
            valueDisplay = String.format("%+.0f", adjustments.blacks * 100),
            onValueChange = { v -> onAdjustmentsChange { it.copy(blacks = v * 2f - 1f) } },
            testTag = "slider_blacks"
        )

        // 8. Saturation (0.0 to 2.0)
        ControlSlider(
            label = "Saturation",
            value = adjustments.saturation / 2f,
            valueDisplay = "${(adjustments.saturation * 100).toInt()}%",
            onValueChange = { v -> onAdjustmentsChange { it.copy(saturation = v * 2f) } },
            testTag = "slider_saturation"
        )

        // 9. Vibrance (-1.0 to 1.0)
        ControlSlider(
            label = "Vibrance",
            value = (adjustments.vibrance + 1f) / 2f,
            valueDisplay = String.format("%+.0f", adjustments.vibrance * 100),
            onValueChange = { v -> onAdjustmentsChange { it.copy(vibrance = v * 2f - 1f) } },
            testTag = "slider_vibrance"
        )

        // 10. Temperature (-1.0 to 1.0)
        ControlSlider(
            label = "Temperature",
            value = (adjustments.temperature + 1f) / 2f,
            valueDisplay = if (adjustments.temperature > 0) "Warm ${String.format("%+.0f", adjustments.temperature * 100)}"
            else "Cool ${String.format("%.0f", adjustments.temperature * 100)}",
            onValueChange = { v -> onAdjustmentsChange { it.copy(temperature = v * 2f - 1f) } },
            testTag = "slider_temperature"
        )

        // 11. Tint (-1.0 to 1.0)
        ControlSlider(
            label = "Tint",
            value = (adjustments.tint + 1f) / 2f,
            valueDisplay = String.format("%+.0f", adjustments.tint * 100),
            onValueChange = { v -> onAdjustmentsChange { it.copy(tint = v * 2f - 1f) } },
            testTag = "slider_tint"
        )

        // 12. Sharpness (0.0 to 1.0)
        ControlSlider(
            label = "Sharpness",
            value = adjustments.sharpness,
            valueDisplay = "${(adjustments.sharpness * 100).toInt()}%",
            onValueChange = { v -> onAdjustmentsChange { it.copy(sharpness = v) } },
            testTag = "slider_sharpness"
        )

        // 13. Fade (0.0 to 1.0)
        ControlSlider(
            label = "Fade",
            value = adjustments.fade,
            valueDisplay = "${(adjustments.fade * 100).toInt()}%",
            onValueChange = { v -> onAdjustmentsChange { it.copy(fade = v) } },
            testTag = "slider_fade"
        )

        // 14. Vignette (0.0 to 1.0)
        ControlSlider(
            label = "Vignette",
            value = adjustments.vignette,
            valueDisplay = "${(adjustments.vignette * 100).toInt()}%",
            onValueChange = { v -> onAdjustmentsChange { it.copy(vignette = v) } },
            testTag = "slider_vignette"
        )

        // 15. Grain (0.0 to 1.0)
        ControlSlider(
            label = "Grain",
            value = adjustments.grain,
            valueDisplay = "${(adjustments.grain * 100).toInt()}%",
            onValueChange = { v -> onAdjustmentsChange { it.copy(grain = v) } },
            testTag = "slider_grain"
        )
    }
}
