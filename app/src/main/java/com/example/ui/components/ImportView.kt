package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentPoseGreen
import com.example.ui.theme.AccentSegBlue
import com.example.ui.theme.ImmersiveDarkBg
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceContainer
import com.example.ui.theme.ImmersiveSurfaceHigh
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun ImportView(
    isAnalyzing: Boolean,
    analysisProgress: Int,
    onVideoSelected: (Uri) -> Unit,
    onLoadSample: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onVideoSelected(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveDarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Glowing App Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(16.dp, CircleShape, spotColor = ImmersivePrimary)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ImmersivePrimary, Color(0xFF4F378B))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ImmersiveOnPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Title & Subtitle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AI Body Tracking",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = ImmersiveTextPrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Video Editor & Camera Follow",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersivePrimary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Automated 33-point body tracking, dynamic virtual camera framing, AI background segmentation, 16 cinema filters, HSL color grading, and 4K Ultra HD export.",
                    fontSize = 12.sp,
                    color = ImmersiveTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Feature Highlights Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeatureChip(label = "ML Kit Pose", color = AccentPoseGreen)
                Spacer(modifier = Modifier.size(8.dp))
                FeatureChip(label = "AI Segmentation", color = AccentSegBlue)
                Spacer(modifier = Modifier.size(8.dp))
                FeatureChip(label = "4K 60FPS", color = ImmersivePrimary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isAnalyzing) {
                // Loading sample progress
                Card(
                    colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = ImmersivePrimary, strokeWidth = 4.dp)
                        Text(
                            text = "Generating & Analyzing Sample...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextPrimary
                        )
                        Text(
                            text = "$analysisProgress%",
                            fontSize = 11.sp,
                            color = ImmersivePrimary
                        )
                    }
                }
            } else {
                // Primary Action Button: Import Video
                Button(
                    onClick = { pickerLauncher.launch("video/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = ImmersivePrimary)
                        .testTag("import_video_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "SELECT VIDEO FROM GALLERY",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Instant Demo Samples Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "OR TEST WITH INSTANT SAMPLES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveTextSecondary,
                        letterSpacing = 0.8.sp
                    )

                    // Sample 1: Street Dance
                    SampleOptionCard(
                        icon = Icons.Default.Nightlife,
                        title = "Sample Dance Video",
                        description = "Human dancer with dynamic arms and body moves across scenery",
                        onClick = { onLoadSample("dance") },
                        testTag = "sample_dance_btn"
                    )

                    // Sample 2: Outdoor Runner
                    SampleOptionCard(
                        icon = Icons.Default.DirectionsRun,
                        title = "Sample Runner Video",
                        description = "Human athlete running with full leg strides & motion tracking",
                        onClick = { onLoadSample("runner") },
                        testTag = "sample_runner_btn"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SampleOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceContainer),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveOutline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ImmersiveSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ImmersiveTextPrimary
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = ImmersiveTextSecondary
                )
            }
        }
    }
}
