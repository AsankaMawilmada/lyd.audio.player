package au.com.inoaspect.lyd.audio.core.data.mediastore

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import au.com.inoaspect.lyd.audio.core.data.lyrics.LrcSidecarReader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deletes a song's sidecar `.lrc` lyrics file. Unlike the audio file, a sidecar isn't scanned
 * into MediaStore.Audio, so its content Uri is looked up from the generic MediaStore.Files
 * collection (which indexes every file under shared storage, not just recognized media types)
 * before going through the same scoped-storage consent flow as [SongDeleter].
 */
@Singleton
class LyricsFileDeleter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** The sidecar `.lrc` path for [songPath], or null if the current song has no deletable lyrics file. */
    fun lyricsFilePath(songPath: String): String? = LrcSidecarReader.find(songPath)?.path

    fun delete(lrcPath: String): DeleteResult {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return try {
                filesUriFor(lrcPath)?.let { context.contentResolver.delete(it, null, null) }
                if (File(lrcPath).delete()) DeleteResult.Deleted else DeleteResult.Failed
            } catch (_: Exception) {
                DeleteResult.Failed
            }
        }
        val uri = filesUriFor(lrcPath) ?: return DeleteResult.Failed
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                DeleteResult.NeedsConsent(pendingIntent.intentSender, retryAfterConsent = false)
            } catch (_: Exception) {
                DeleteResult.Failed
            }
        } else {
            try {
                context.contentResolver.delete(uri, null, null)
                DeleteResult.Deleted
            } catch (e: RecoverableSecurityException) {
                DeleteResult.NeedsConsent(e.userAction.actionIntent.intentSender, retryAfterConsent = true)
            } catch (_: Exception) {
                DeleteResult.Failed
            }
        }
    }

    /** Re-attempts the delete after the user grants consent via the RecoverableSecurityException flow (API 29 only). */
    fun retryAfterConsent(lrcPath: String): Boolean {
        val uri = filesUriFor(lrcPath) ?: return false
        return try {
            context.contentResolver.delete(uri, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun filesUriFor(path: String): Uri? {
        val collection = MediaStore.Files.getContentUri("external")
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Files.FileColumns._ID),
            "${MediaStore.Files.FileColumns.DATA} = ?",
            arrayOf(path),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }
}
