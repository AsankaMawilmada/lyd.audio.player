package com.lyd.player.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.lyd.player.core.data.art.ArtworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ArtworkViewModel @Inject constructor(
    val repository: ArtworkRepository,
) : ViewModel()

/** Resolves (and caches to disk, once) the artwork file for an album, or null if it has none. */
@Composable
fun rememberArtFile(albumId: Long, representativeSongMediaStoreId: Long, viewModel: ArtworkViewModel = hiltViewModel()): File? {
    var file by remember(albumId) { mutableStateOf<File?>(null) }
    LaunchedEffect(albumId, representativeSongMediaStoreId) {
        file = viewModel.repository.getArtFile(albumId, representativeSongMediaStoreId)
    }
    return file
}
