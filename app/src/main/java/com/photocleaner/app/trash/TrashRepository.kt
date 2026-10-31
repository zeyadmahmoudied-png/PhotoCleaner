package com.photocleaner.app.trash

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.photocleaner.app.data.local.AppDatabase
import com.photocleaner.app.data.local.TrashEntity
import com.photocleaner.app.domain.model.PhotoItem
import com.photocleaner.app.domain.model.TrashItem
import com.photocleaner.app.image.ImageHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/**
 * Permanent trash with strict safety:
 * 1. Copy into app-private storage
 * 2. Verify size + SHA-256
 * 3. Persist metadata
 * 4. Only then attempt MediaStore delete of original
 * If anything fails, original is left untouched.
 */
class TrashRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context)
) {

    private val trashDir: File by lazy {
        File(context.filesDir, "permanent_trash").also { it.mkdirs() }
    }

    fun observeTrash(): Flow<List<TrashItem>> =
        db.trashDao().getAll().map { list ->
            list.map { e ->
                TrashItem(
                    id = e.id,
                    originalUri = e.originalUri,
                    originalPath = e.originalPath,
                    displayName = e.displayName,
                    sizeBytes = e.sizeBytes,
                    albumName = e.albumName,
                    deletedAt = e.deletedAt,
                    trashFilePath = e.trashFilePath,
                    sha256 = e.sha256
                )
            }
        }

    suspend fun moveToTrash(photo: PhotoItem): Result<TrashItem> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(photo.uri)
            val trashId = UUID.randomUUID().toString()
            val destFile = File(trashDir, "${trashId}_${sanitize(photo.displayName)}")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(
                IllegalStateException("Cannot open original stream: ${photo.uri}")
            )

            if (destFile.length() != photo.sizeBytes && photo.sizeBytes > 0) {
                destFile.delete()
                return@withContext Result.failure(
                    IllegalStateException(
                        "Size mismatch after copy: expected ${photo.sizeBytes}, got ${destFile.length()}"
                    )
                )
            }

            val copySha = FileInputStream(destFile).use { ImageHasher.sha256(it) }
            if (photo.sha256 != null && photo.sha256 != copySha) {
                destFile.delete()
                return@withContext Result.failure(
                    IllegalStateException("SHA-256 mismatch after copy — original left intact")
                )
            }

            val entity = TrashEntity(
                id = trashId,
                originalUri = photo.uri,
                originalPath = photo.path,
                displayName = photo.displayName,
                sizeBytes = destFile.length(),
                albumName = photo.albumName,
                deletedAt = System.currentTimeMillis(),
                trashFilePath = destFile.absolutePath,
                sha256 = copySha
            )
            db.trashDao().insert(entity)

            // Attempt delete; failure does not roll back the safe copy
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }

            Result.success(
                TrashItem(
                    id = entity.id,
                    originalUri = entity.originalUri,
                    originalPath = entity.originalPath,
                    displayName = entity.displayName,
                    sizeBytes = entity.sizeBytes,
                    albumName = entity.albumName,
                    deletedAt = entity.deletedAt,
                    trashFilePath = entity.trashFilePath,
                    sha256 = entity.sha256
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restore(item: TrashItem, preferredName: String? = null): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val src = File(item.trashFilePath)
                if (!src.exists() || !src.canRead()) {
                    return@withContext Result.failure(
                        IllegalStateException("Trash file missing or unreadable")
                    )
                }

                val currentSha = FileInputStream(src).use { ImageHasher.sha256(it) }
                if (item.sha256 != null && item.sha256 != currentSha) {
                    return@withContext Result.failure(
                        IllegalStateException("Trash file integrity check failed")
                    )
                }

                val name = preferredName ?: item.displayName
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, guessMime(name))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/PhotoCleanerRestored"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val newUri = context.contentResolver.insert(collection, values)
                    ?: return@withContext Result.failure(IllegalStateException("MediaStore insert failed"))

                context.contentResolver.openOutputStream(newUri)?.use { out ->
                    FileInputStream(src).use { inp -> inp.copyTo(out) }
                } ?: run {
                    context.contentResolver.delete(newUri, null, null)
                    return@withContext Result.failure(IllegalStateException("Cannot write restored file"))
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(newUri, values, null, null)
                }

                src.delete()
                db.trashDao().deleteById(item.id)
                Result.success(newUri)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deletePermanently(item: TrashItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(item.trashFilePath).delete()
            db.trashDao().deleteById(item.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._\\-\\u0600-\\u06FF]"), "_").take(120)

    private fun guessMime(name: String): String = when {
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".webp", true) -> "image/webp"
        name.endsWith(".gif", true) -> "image/gif"
        else -> "image/jpeg"
    }
}
