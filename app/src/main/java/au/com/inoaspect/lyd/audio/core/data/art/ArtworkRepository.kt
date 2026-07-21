package au.com.inoaspect.lyd.audio.core.data.art

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import au.com.inoaspect.lyd.audio.core.data.db.ArtCacheDao
import au.com.inoaspect.lyd.audio.core.data.db.ArtCacheEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches album art from the MediaStore once per album id and caches it to a file under
 * the app's private storage. Albums confirmed to have no artwork are remembered so we never
 * re-issue the (relatively expensive) MediaStore lookup for them again.
 */
@Singleton
class ArtworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artCacheDao: ArtCacheDao,
) {
    private val cacheDir: File by lazy {
        File(context.filesDir, "art_cache").apply { mkdirs() }
    }

    suspend fun getArtFile(albumId: Long, representativeSongMediaStoreId: Long): File? =
        withContext(Dispatchers.IO) {
            val cached = artCacheDao.getEntry(albumId)
            if (cached != null) {
                if (!cached.hasArt) return@withContext null
                val file = cached.filePath?.let(::File)
                if (file != null && file.exists()) return@withContext file
                // Cached file vanished (e.g. cleared app storage) — fall through and refetch.
            }

            val bitmap = fetchBitmap(albumId, representativeSongMediaStoreId)
            if (bitmap == null) {
                artCacheDao.upsert(ArtCacheEntity(albumId, null, hasArt = false))
                return@withContext null
            }

            val file = File(cacheDir, "$albumId.jpg")
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            artCacheDao.upsert(ArtCacheEntity(albumId, file.absolutePath, hasArt = true))
            file
        }

    private fun fetchBitmap(albumId: Long, representativeSongMediaStoreId: Long): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val songUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                representativeSongMediaStoreId,
            )
            context.contentResolver.loadThumbnail(songUri, Size(512, 512), null)
        } else {
            @Suppress("DEPRECATION")
            val albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId,
            )
            context.contentResolver.openInputStream(albumArtUri)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (_: Exception) {
        null
    }
}
