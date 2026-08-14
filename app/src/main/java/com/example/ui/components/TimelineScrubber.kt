package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraKeyframe
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceContainer
import com.example.ui.theme.ImmersiveSurfaceHigh
import com.example.ui.theme.ImmersiveTextPrimary

@Composable
fun TimelineScrubber(
    currentMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    keyframes: List<CameraKeyframe>,
    onSeek: (Long) -> Unit,
    onStepPrev: () -> Unit,
    onStepNext: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current.density

    val safeDuration = totalDurationMs.coerceAtLeast(1L)
    val progress = (currentMs.toFloat() / safeDuration).coerceIn(0f, 1f)

    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val hundredths = (ms % 1000) / 10
        return String.format("%02d:%02d.%02d", min, sec, hundredths)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ImmersiveSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Top: Timecode and Scrubber Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Time Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ImmersivePrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatTime(currentMs),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary
                    )
                }

                // Scrubber Track
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val norm = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                                onSeek((norm * safeDuration).toLong())
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val norm = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                onSeek((norm * safeDuration).toLong())
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Background track line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ImmersiveOutline)
                    )

                    // Active progress line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ImmersivePrimary)
                    )

                    // Keyframe Dots on Scrubber
                    for (kf in keyframes) {
                        val kfPos = (kf.timestampMs.toFloat() / safeDuration).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = ((kfPos * (trackWidthPx / density)) - 3).dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (kf.isAutoGenerated) Color.White.copy(alpha = 0.6f) else ImmersivePrimary)
                        )
                    }

                    // Scrubber Thumb
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = ((progress * (trackWidthPx / density)) - 8).dp)
                            .size(16.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Total Duration
                Text(
                    text = formatTime(totalDurationMs),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Bottom: Media Controls (Prev Frame, Play/Pause, Next Frame)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Previous
                IconButton(
                    onClick = onStepPrev,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ImmersiveSurfaceHigh)
                        .testTag("prev_frame_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Step Previous",
                        tint = ImmersiveTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .size(48.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(ImmersivePrimary)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = ImmersiveOnPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Step Next
                IconButton(
                    onClick = onStepNext,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ImmersiveSurfaceHigh)
                        .testTag("next_frame_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Step Next",
                        tint = ImmersiveTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
