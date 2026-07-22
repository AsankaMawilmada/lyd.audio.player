package au.com.inoaspect.lyd.audio.playback

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> ListenableFuture<T>.awaitFuture(executor: Executor): T =
    suspendCancellableCoroutine { cont ->
        addListener(
            {
                try {
                    cont.resume(get())
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            },
            executor,
        )
        cont.invokeOnCancellation { cancel(false) }
    }

/**
 * Bridges a suspend computation into the [ListenableFuture] that Media3's [MediaLibraryService]
 * callbacks require, since those are invoked off the main/coroutine world.
 */
fun <T> CoroutineScope.future(block: suspend () -> T): ListenableFuture<T> {
    val result = SettableFuture.create<T>()
    launch {
        try {
            result.set(block())
        } catch (e: Exception) {
            result.setException(e)
        }
    }
    return result
}
