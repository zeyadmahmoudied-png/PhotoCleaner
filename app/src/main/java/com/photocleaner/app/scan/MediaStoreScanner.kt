package com.photocleaner.app.scan

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photocleaner.app.domain.model.PhotoItem
import com.photocleaner.app.image.ImageHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

/**
 * Read-only MediaStore scanner.
 * Never deletes or modifies user media.
 */
class MediaStoreScanner(private val context: Context) {

    data class ScanResult(
        val items: List<PhotoItem>,
        val processed: Int,
        val totalEstimate: Int,
        val phase: String
    )

    fun scanImages(
        batchSize: Int = 40,
        computeHashes: Boolean = true
    ): Flow<ScanResult> = flow {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val all = mutableListOf<PhotoItem>()

        context.contentResolver.query(
            collection, projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val wCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val hCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            val total = cursor.count
            var processed = 0
            emit(ScanResult(emptyList(), 0, total, "listing"))

            while (cursor.moveToNext()) {
                coroutineContext.ensureActive()

                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val name = cursor.getString(nameCol) ?: "unknown"
                val size = cursor.getLong(sizeCol)
                val w = cursor.getInt(wCol)
                val h = cursor.getInt(hCol)
                val date = cursor.getLong(dateCol).takeIf { it > 0 }
                val album = cursor.getString(bucketCol)
                val path = try { cursor.getString(dataCol) } catch (_: Exception) { null }

                var sha: String? = null
                var phash: String? = null

                if (computeHashes) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            sha = ImageHasher.sha256(bytes)
                        }
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = ImageHasher.decodeSampled(stream, 256)
                            if (bmp != null) {
                                phash = ImageHasher.dHash(bmp)
                                bmp.recycle()
                            }
                        }
                    } catch (_: IOException) {
                    } catch (_: SecurityException) {
                    } catch (_: OutOfMemoryError) {
                    }
                }

                all.add(
                    PhotoItem(
                        id = id.toString(),
                        uri = uri.toString(),
                        path = path,
                        displayName = name,
                        sizeBytes = size,
                        width = w,
                        height = h,
                        dateTaken = date,
                        albumName = album,
                        sha256 = sha,
                        perceptualHash = phash
                    )
                )
                processed++

                if (processed % batchSize == 0 || processed == total) {
                    emit(ScanResult(all.toList(), processed, total, "hashing"))
                }
            }
        }

        emit(ScanResult(all.toList(), all.size, all.size, "done"))
    }.flowOn(Dispatchers.IO)

    suspend fun listImageMetadataOnly(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PhotoItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val wCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val hCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                result.add(
                    PhotoItem(
                        id = id.toString(),
                        uri = uri.toString(),
                        displayName = cursor.getString(nameCol) ?: "unknown",
                        sizeBytes = cursor.getLong(sizeCol),
                        width = cursor.getInt(wCol),
                        height = cursor.getInt(hCol),
                        dateTaken = cursor.getLong(dateCol).takeIf { it > 0 },
                        albumName = cursor.getString(bucketCol)
                    )
                )
            }
        }
        result
    }
}
