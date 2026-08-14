package com.example.model

import android.net.Uri

data class VideoMetadata(
    val uri: Uri,
    val title: String = "Imported Video",
    val durationMs: Long = 0L,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Float = 30f,
    val rotation: Int = 0,
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "video/mp4",
    val isDemoSample: Boolean = false
) {
    val resolutionLabel: String
        get() {
            val maxDim = maxOf(width, height)
            return when {
                maxDim >= 3800 -> "4K UHD (${width}x${height})"
                maxDim >= 2500 -> "2K QHD (${width}x${height})"
                maxDim >= 1900 -> "1080p FHD (${width}x${height})"
                maxDim >= 1200 -> "720p HD (${width}x${height})"
                else -> "${width}x${height}"
            }
        }

    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hundredths = (durationMs % 1000) / 10
            return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
        }

    val fileSizeFormatted: String
        get() {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                val kb = fileSizeBytes / 1024.0
                String.format("%.1f KB", kb)
            }
        }
}
