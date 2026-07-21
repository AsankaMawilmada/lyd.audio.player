package au.com.inoaspect.lyd.audio.core.data.mediastore

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import au.com.inoaspect.lyd.audio.core.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data class NeedsConsent(val intentSender: IntentSender, val retryAfterConsent: Boolean) : DeleteResult
    data object Failed : DeleteResult
}

/**
 * Deletes a song's underlying file from the device. Since these files are scanned from the
 * MediaStore (not created by this app), scoped storage requires going through
 * [MediaStore.createDeleteRequest] (API 30+) or catching [RecoverableSecurityException] (API 29)
 * to get user consent before the delete actually takes effect.
 */
@Singleton
class SongDeleter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun delete(song: Song): DeleteResult {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.mediaStoreId)
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                DeleteResult.NeedsConsent(pendingIntent.intentSender, retryAfterConsent = false)
            } catch (_: Exception) {
                DeleteResult.Failed
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> try {
                context.contentResolver.delete(uri, null, null)
                DeleteResult.Deleted
            } catch (e: RecoverableSecurityException) {
                DeleteResult.NeedsConsent(e.userAction.actionIntent.intentSender, retryAfterConsent = true)
            } catch (_: Exception) {
                DeleteResult.Failed
            }
            else -> try {
                context.contentResolver.delete(uri, null, null)
                runCatching { File(song.path).delete() }
                DeleteResult.Deleted
            } catch (_: Exception) {
                DeleteResult.Failed
            }
        }
    }

    /** Re-attempts the delete after the user grants consent via the RecoverableSecurityException flow (API 29 only). */
    fun retryAfterConsent(song: Song): Boolean {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.mediaStoreId)
        return try {
            context.contentResolver.delete(uri, null, null)
            runCatching { File(song.path).delete() }
            true
        } catch (_: Exception) {
            false
        }
    }
}
