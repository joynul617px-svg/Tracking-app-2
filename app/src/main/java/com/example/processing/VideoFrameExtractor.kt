package com.example.processing

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoFrameExtractor(private val context: Context) {

    suspend fun extractMetadata(uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val mimeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            val durationMs = durationStr?.toLongOrNull() ?: 6000L
            var width = widthStr?.toIntOrNull() ?: 1280
            var height = heightStr?.toIntOrNull() ?: 720
            val rotation = rotationStr?.toIntOrNull() ?: 0

            // If video is rotated 90 or 270 degrees, swap width and height
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }

            var fileSize = 0L
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    fileSize = pfd.statSize
                }
            } catch (e: Exception) {
                fileSize = 0L
            }

            VideoMetadata(
                uri = uri,
                title = uri.lastPathSegment ?: "Imported Video",
                durationMs = durationMs,
                width = width,
                height = height,
                fps = 30f,
                rotation = rotation,
                fileSizeBytes = fileSize,
                mimeType = mimeStr
            )
        } finally {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    retriever.close()
                } else {
                    retriever.release()
                }
            } catch (e: Exception) {}
        }
    }

    suspend fun getFrameAt(uri: Uri, timestampMs: Long, targetWidth: Int = -1, targetHeight: Int = -1): Bitmap? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val timeUs = timestampMs * 1000L
                val rawFrame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
                    targetWidth > 0 && targetHeight > 0) {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST,
                        targetWidth,
                        targetHeight
                    ) ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                }

                // Copy to clean ARGB_8888 software bitmap to prevent ashmem pinning deprecation and native buffer locks
                if (rawFrame != null) {
                    val safeCopy = rawFrame.copy(Bitmap.Config.ARGB_8888, true)
                    rawFrame.recycle()
                    safeCopy
                } else {
                    // Fallback generator for demo samples if container decoder returns null
                    val path = uri.path ?: ""
                    if (path.contains("sample_dance") || path.contains("dance")) {
                        SampleVideoGenerator.generateDirectFrameBitmap("dance", timestampMs / 1000f, targetWidth.coerceAtLeast(640), targetHeight.coerceAtLeast(360))
                    } else if (path.contains("sample_runner") || path.contains("runner")) {
                        SampleVideoGenerator.generateDirectFrameBitmap("runner", timestampMs / 1000f, targetWidth.coerceAtLeast(640), targetHeight.coerceAtLeast(360))
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                val path = uri.path ?: ""
                if (path.contains("sample_dance") || path.contains("dance")) {
                    SampleVideoGenerator.generateDirectFrameBitmap("dance", timestampMs / 1000f, targetWidth.coerceAtLeast(640), targetHeight.coerceAtLeast(360))
                } else if (path.contains("sample_runner") || path.contains("runner")) {
                    SampleVideoGenerator.generateDirectFrameBitmap("runner", timestampMs / 1000f, targetWidth.coerceAtLeast(640), targetHeight.coerceAtLeast(360))
                } else {
                    null
                }
            } finally {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        retriever.close()
                    } else {
                        retriever.release()
                    }
                } catch (e: Exception) {}
            }
        }
}
