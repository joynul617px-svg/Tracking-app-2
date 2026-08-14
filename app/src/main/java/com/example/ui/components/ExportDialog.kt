package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ExportConfig
import com.example.model.ExportFps
import com.example.model.ExportQuality
import com.example.model.ExportResolution
import com.example.model.ProcessProgress
import com.example.model.VideoMetadata
import com.example.ui.theme.AccentPoseGreen
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceContainer
import com.example.ui.theme.ImmersiveSurfaceHigh
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun ExportDialog(
    metadata: VideoMetadata?,
    config: ExportConfig,
    isExporting: Boolean,
    progress: ProcessProgress,
    exportedUri: Uri?,
    onConfigChange: ((ExportConfig) -> ExportConfig) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ImmersiveSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveOutline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            if (exportedUri != null) {
                // Success State
                ExportSuccessView(
                    savedUri = exportedUri,
                    context = context,
                    onDismiss = onDismiss
                )
            } else if (isExporting) {
                // In-Progress State
                ExportProgressView(
                    progress = progress,
                    onCancel = onCancelExport
                )
            } else {
                // Config Setup State
                ExportSetupView(
                    metadata = metadata,
                    config = config,
                    onConfigChange = onConfigChange,
                    onStartExport = onStartExport,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ExportSetupView(
    metadata: VideoMetadata?,
    config: ExportConfig,
    onConfigChange: ((ExportConfig) -> ExportConfig) -> Unit,
    onStartExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val durationSec = (metadata?.durationMs ?: 0L) / 1000f
    val (w, h) = config.resolution.getDimensions(metadata?.width ?: 1920, metadata?.height ?: 1080)
    val fpsVal = if (config.fps.fpsValue > 0) config.fps.fpsValue else 30
    val estSizeMb = config.estimateFileSizeMb(durationSec, w, h, fpsVal)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPORT SETTINGS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersivePrimary,
                letterSpacing = 0.8.sp
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Close", tint = ImmersiveTextSecondary)
            }
        }

        // Resolution Options
        Text(text = "RESOLUTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveTextSecondary)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (res in listOf(
                ExportResolution.RES_4K,
                ExportResolution.RES_1440P,
                ExportResolution.RES_1080P,
                ExportResolution.RES_720P,
                ExportResolution.RES_480P
            )) {
                val isSelected = config.resolution == res
                OptionCard(
                    title = res.label,
                    badge = if (res.isUhd) "ULTRA HD" else null,
                    isSelected = isSelected,
                    onClick = { onConfigChange { it.copy(resolution = res) } },
                    testTag = "export_res_${res.name.lowercase()}"
                )
            }
        }

        // Frame Rate Options
        Text(text = "FRAME RATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveTextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (fps in ExportFps.entries) {
                val isSelected = config.fps == fps
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ImmersivePrimary.copy(alpha = 0.25f) else ImmersiveSurfaceHigh)
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, ImmersivePrimary, RoundedCornerShape(8.dp))
                            else Modifier.border(1.dp, ImmersiveOutline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        )
                        .clickable { onConfigChange { it.copy(fps = fps) } }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (fps == ExportFps.ORIGINAL) "Orig" else "${fps.fpsValue} FPS",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ImmersivePrimary else ImmersiveTextPrimary
                    )
                }
            }
        }

        // Quality
        Text(text = "ENCODING QUALITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveTextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (quality in ExportQuality.entries) {
                val isSelected = config.quality == quality
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ImmersivePrimary.copy(alpha = 0.25f) else ImmersiveSurfaceHigh)
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, ImmersivePrimary, RoundedCornerShape(8.dp))
                            else Modifier.border(1.dp, ImmersiveOutline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        )
                        .clickable { onConfigChange { it.copy(quality = quality) } }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quality.label.split(" ").first(),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ImmersivePrimary else ImmersiveTextPrimary
                    )
                }
            }
        }

        // Toggles: Mute Audio & Overlay
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Mute Audio", fontSize = 12.sp, color = ImmersiveTextPrimary)
            Switch(
                checked = config.muteAudio,
                onCheckedChange = { checked -> onConfigChange { it.copy(muteAudio = checked) } },
                colors = SwitchDefaults.colors(checkedThumbColor = ImmersivePrimary, checkedTrackColor = ImmersivePrimary.copy(alpha = 0.3f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Include Skeleton Overlay", fontSize = 12.sp, color = ImmersiveTextPrimary)
            Switch(
                checked = config.exportTrackingOverlay,
                onCheckedChange = { checked -> onConfigChange { it.copy(exportTrackingOverlay = checked) } },
                colors = SwitchDefaults.colors(checkedThumbColor = ImmersivePrimary, checkedTrackColor = ImmersivePrimary.copy(alpha = 0.3f))
            )
        }

        // Estimated Output details
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Est. Output Size:", fontSize = 11.sp, color = ImmersiveTextSecondary)
                Text(
                    text = "${String.format("%.1f", estSizeMb)} MB (${w}x${h})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersivePrimary
                )
            }
        }

        // Action Button
        Button(
            onClick = onStartExport,
            colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary, contentColor = ImmersiveOnPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("start_export_btn")
        ) {
            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
            Text(
                text = if (config.resolution.isUhd) "EXPORT 4K ULTRA HD" else "START EXPORT",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun OptionCard(
    title: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) ImmersivePrimary.copy(alpha = 0.20f) else ImmersiveSurfaceHigh)
            .then(
                if (isSelected) Modifier.border(1.5.dp, ImmersivePrimary, RoundedCornerShape(10.dp))
                else Modifier.border(1.dp, ImmersiveOutline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) ImmersivePrimary else ImmersiveTextPrimary
            )
            if (badge != null) {
                Surface(
                    color = ImmersivePrimary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = ImmersiveOnPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportProgressView(
    progress: ProcessProgress,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            progress = { progress.progressPercent / 100f },
            color = ImmersivePrimary,
            trackColor = ImmersiveSurfaceHigh,
            strokeWidth = 6.dp,
            modifier = Modifier.size(72.dp)
        )

        Text(
            text = progress.stageTitle.ifEmpty { "Exporting Video..." },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ImmersiveTextPrimary
        )

        Text(
            text = "${progress.progressPercent}%",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = ImmersivePrimary
        )

        Text(
            text = progress.details,
            fontSize = 11.sp,
            color = ImmersiveTextSecondary
        )

        LinearProgressIndicator(
            progress = { progress.progressPercent / 100f },
            color = ImmersivePrimary,
            trackColor = ImmersiveSurfaceHigh,
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextSecondary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Cancel Export", fontSize = 11.sp)
        }
    }
}

@Composable
private fun ExportSuccessView(
    savedUri: Uri,
    context: Context,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = AccentPoseGreen,
            modifier = Modifier.size(64.dp)
        )

        Text(
            text = "Export Completed!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ImmersiveTextPrimary
        )

        Text(
            text = "Your AI body tracked video has been encoded and saved to the device Movies gallery.",
            fontSize = 11.sp,
            color = ImmersiveTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(savedUri, "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try { context.startActivity(intent) } catch (e: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveSurfaceHigh),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                Text("Open", fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_STREAM, savedUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try { context.startActivity(Intent.createChooser(intent, "Share Video")) } catch (e: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary, contentColor = ImmersiveOnPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                Text("Share", fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }

        OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done", fontSize = 12.sp, color = ImmersivePrimary)
        }
    }
}
