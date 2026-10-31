package com.photocleaner.app.domain.model

import java.util.UUID

enum class PhotoDecision {
    KEEP,
    SUGGEST_DELETE,
    UNDECIDED
}

enum class SimilarityLevel {
    EXACT,
    VERY_HIGH,
    MEDIUM,
    LOW
}

data class PhotoItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val path: String? = null,
    val displayName: String,
    val sizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
    val dateTaken: Long? = null,
    val albumName: String? = null,
    val sha256: String? = null,
    val perceptualHash: String? = null,
    val decision: PhotoDecision = PhotoDecision.UNDECIDED,
    val isBlurry: Boolean = false,
    val isScreenshot: Boolean = false
)

data class DuplicateGroup(
    val id: String = UUID.randomUUID().toString(),
    val type: SimilarityLevel,
    val photos: List<PhotoItem>,
    val estimatedSavingsBytes: Long = 0L
) {
    val keepCount: Int get() = photos.count { it.decision == PhotoDecision.KEEP }
    val deleteCount: Int get() = photos.count { it.decision == PhotoDecision.SUGGEST_DELETE }

    fun hasAtLeastOneKeep(): Boolean = keepCount >= 1

    fun withDecision(photoId: String, decision: PhotoDecision): DuplicateGroup {
        val updated = photos.map {
            if (it.id == photoId) it.copy(decision = decision) else it
        }
        return copy(photos = updated)
    }
}

data class ScanProgress(
    val total: Int = 0,
    val processed: Int = 0,
    val currentPhase: String = "",
    val isPaused: Boolean = false,
    val isCancelled: Boolean = false,
    val elapsedMs: Long = 0L
) {
    val remaining: Int get() = (total - processed).coerceAtLeast(0)
    val progressFraction: Float get() = if (total == 0) 0f else processed.toFloat() / total
}

data class TrashItem(
    val id: String = UUID.randomUUID().toString(),
    val originalUri: String,
    val originalPath: String?,
    val displayName: String,
    val sizeBytes: Long,
    val albumName: String?,
    val deletedAt: Long = System.currentTimeMillis(),
    val trashFilePath: String,
    val sha256: String?
)

data class AppStats(
    val totalPhotos: Int = 0,
    val totalSizeBytes: Long = 0L,
    val exactDuplicateGroups: Int = 0,
    val exactDuplicatePhotos: Int = 0,
    val similarGroups: Int = 0,
    val similarPhotos: Int = 0,
    val trashCount: Int = 0,
    val trashSizeBytes: Long = 0L,
    val spaceSavedBytes: Long = 0L,
    val lastScanTimestamp: Long? = null
)
