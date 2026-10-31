package com.photocleaner.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_items")
data class PhotoEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val path: String?,
    val displayName: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateTaken: Long?,
    val albumName: String?,
    val sha256: String?,
    val perceptualHash: String?,
    val decision: String,
    val groupId: String?,
    val isBlurry: Boolean = false,
    val isScreenshot: Boolean = false
)

@Entity(tableName = "duplicate_groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val type: String,
    val estimatedSavingsBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trash_items")
data class TrashEntity(
    @PrimaryKey val id: String,
    val originalUri: String,
    val originalPath: String?,
    val displayName: String,
    val sizeBytes: Long,
    val albumName: String?,
    val deletedAt: Long,
    val trashFilePath: String,
    val sha256: String?
)

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val totalPhotos: Int,
    val processedPhotos: Int,
    val status: String
)
