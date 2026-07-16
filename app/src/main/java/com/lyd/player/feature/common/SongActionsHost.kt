package com.lyd.player.feature.common

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SongActionsHost(state: SongActionsState, viewModel: SongActionsViewModel = hiltViewModel()) {
    var pendingConsent by remember { mutableStateOf<DeleteConsentRequest?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        pendingConsent?.let { viewModel.onDeleteConsentResult(it, result.resultCode == Activity.RESULT_OK) }
        pendingConsent = null
    }

    LaunchedEffect(viewModel) {
        viewModel.deleteConsentRequest.collect { request ->
            pendingConsent = request
            launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        }
    }

    state.menuSong?.let { song ->
        SongActionsSheet(
            song = song,
            viewModel = viewModel,
            onDismiss = { state.menuSong = null },
            onAddToPlaylist = {
                state.addToPlaylistPaths = listOf(song.path)
                state.menuSong = null
            },
        )
    }
    state.addToPlaylistPaths?.let { paths ->
        AddToPlaylistSheet(songPaths = paths, onDismiss = { state.addToPlaylistPaths = null })
    }
}
