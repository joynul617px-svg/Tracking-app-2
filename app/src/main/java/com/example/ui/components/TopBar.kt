package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoMetadata
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun TopBar(
    metadata: VideoMetadata?,
    canUndo: Boolean,
    canRedo: Boolean,
    onBackClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ImmersiveSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back & Title info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ImmersiveTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "AI Body Tracking",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersiveTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    if (metadata != null) {
                        val maxDim = maxOf(metadata.width, metadata.height)
                        val resTag = when {
                            maxDim >= 3800 -> "4K UHD"
                            maxDim >= 2500 -> "2K QHD"
                            maxDim >= 1900 -> "1080p FHD"
                            else -> "720p HD"
                        }
                        Text(
                            text = "$resTag • ${metadata.fps.toInt()} FPS • ${metadata.durationFormatted}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = ImmersiveTextSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // Right: Undo/Redo & Export Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onUndoClick,
                    enabled = canUndo,
                    modifier = Modifier.size(36.dp).testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) ImmersiveTextPrimary else ImmersiveOutline,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRedoClick,
                    enabled = canRedo,
                    modifier = Modifier.size(36.dp).testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) ImmersiveTextPrimary else ImmersiveOutline,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onExportClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .testTag("export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text(
                        text = "EXPORT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}
