package com.lyd.player.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lyd.player.core.design.BottomNavBar
import com.lyd.player.core.design.BottomNavItem
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.MiniPlayer
import com.lyd.player.feature.equalizer.EqualizerScreen
import com.lyd.player.feature.home.HomeScreen
import com.lyd.player.feature.library.AlbumDetailScreen
import com.lyd.player.feature.library.ArtistDetailScreen
import com.lyd.player.feature.library.FolderDetailScreen
import com.lyd.player.feature.library.LibraryScreen
import com.lyd.player.feature.nowplaying.NowPlayingScreen
import com.lyd.player.feature.playlists.CreatePlaylistScreen
import com.lyd.player.feature.playlists.PlaylistDetailScreen
import com.lyd.player.feature.playlists.PlaylistsScreen
import com.lyd.player.feature.search.SearchScreen
import com.lyd.player.feature.sleeptimer.SleepTimerSheet

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun LydApp() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    var showSleepTimer by remember { mutableStateOf(false) }

    MediaPermissionGate(onPermissionGranted = appViewModel::onPermissionGranted) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showChrome = currentRoute == null ||
            currentRoute == Routes.HOME ||
            currentRoute.startsWith("library") ||
            currentRoute == Routes.PLAYLISTS ||
            currentRoute == Routes.SEARCH
        val playbackState by appViewModel.playbackState.collectAsState()

        Scaffold(
            containerColor = LydColors.Background,
            modifier = Modifier.statusBarsPadding(),
            bottomBar = {
                if (showChrome) {
                    Column(Modifier.navigationBarsPadding()) {
                        if (playbackState.hasMedia) {
                            MiniPlayer(
                                title = playbackState.currentItem?.title.orEmpty(),
                                artist = playbackState.currentItem?.artist.orEmpty(),
                                artFile = playbackState.currentItem?.artworkUri,
                                isPlaying = playbackState.isPlaying,
                                progressFraction = if (playbackState.durationMs > 0) {
                                    playbackState.positionMs.toFloat() / playbackState.durationMs
                                } else {
                                    0f
                                },
                                onClick = { navController.navigate(Routes.NOW_PLAYING) },
                                onTogglePlayPause = appViewModel.playerController::togglePlayPause,
                                onNext = appViewModel.playerController::next,
                                modifier = Modifier.padding(horizontal = LydSpacing.safeArea, vertical = LydSpacing.sm),
                            )
                        }
                        BottomNavBar(
                            items = listOf(
                                BottomNavItem("Listen", Icons.Filled.PlayCircle, currentRoute == Routes.HOME) {
                                    navController.navigateToTab(Routes.HOME)
                                },
                                BottomNavItem("Library", Icons.Filled.LibraryMusic, currentRoute?.startsWith("library") == true) {
                                    navController.navigateToTab(Routes.library())
                                },
                                BottomNavItem("Playlists", Icons.AutoMirrored.Filled.PlaylistPlay, currentRoute == Routes.PLAYLISTS) {
                                    navController.navigateToTab(Routes.PLAYLISTS)
                                },
                                BottomNavItem("Search", Icons.Filled.Search, currentRoute == Routes.SEARCH) {
                                    navController.navigateToTab(Routes.SEARCH)
                                },
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenSearch = { navController.navigateToTab(Routes.SEARCH) },
                        onOpenLibrary = { tab -> navController.navigateToTab(Routes.library(tab)) },
                        onOpenEqualizer = { navController.navigate(Routes.EQUALIZER) },
                        onOpenSleepTimer = { showSleepTimer = true },
                    )
                }
                composable(
                    route = Routes.LIBRARY,
                    arguments = listOf(navArgument(Routes.LIBRARY_TAB_ARG) { type = NavType.StringType; defaultValue = "songs" }),
                ) {
                    LibraryScreen(
                        onOpenAlbum = { id -> navController.navigate(Routes.albumDetail(id)) },
                        onOpenArtist = { id -> navController.navigate(Routes.artistDetail(id)) },
                        onOpenFolder = { path -> navController.navigate(Routes.folderDetail(path)) },
                    )
                }
                composable(Routes.PLAYLISTS) {
                    PlaylistsScreen(
                        onOpenPlaylist = { id -> navController.navigate(Routes.playlistDetail(id)) },
                        onCreatePlaylist = { navController.navigate(Routes.CREATE_PLAYLIST) },
                    )
                }
                composable(Routes.SEARCH) {
                    SearchScreen(onOpenLibrary = { tab -> navController.navigateToTab(Routes.library(tab)) })
                }
                composable(
                    route = Routes.ALBUM_DETAIL,
                    arguments = listOf(navArgument(Routes.ALBUM_ID_ARG) { type = NavType.LongType }),
                ) {
                    AlbumDetailScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = Routes.ARTIST_DETAIL,
                    arguments = listOf(navArgument(Routes.ARTIST_ID_ARG) { type = NavType.LongType }),
                ) {
                    ArtistDetailScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = Routes.FOLDER_DETAIL,
                    arguments = listOf(navArgument(Routes.FOLDER_PATH_ARG) { type = NavType.StringType }),
                ) {
                    FolderDetailScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = Routes.PLAYLIST_DETAIL,
                    arguments = listOf(navArgument(Routes.PLAYLIST_ID_ARG) { type = NavType.LongType }),
                ) {
                    PlaylistDetailScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.CREATE_PLAYLIST) {
                    CreatePlaylistScreen(
                        onBack = { navController.popBackStack() },
                        onCreated = { id ->
                            navController.popBackStack()
                            navController.navigate(Routes.playlistDetail(id))
                        },
                    )
                }
                composable(Routes.NOW_PLAYING) {
                    NowPlayingScreen(
                        onCollapse = { navController.popBackStack() },
                        onOpenEqualizer = { navController.navigate(Routes.EQUALIZER) },
                    )
                }
                composable(Routes.EQUALIZER) {
                    EqualizerScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }

    if (showSleepTimer) {
        SleepTimerSheet(onDismiss = { showSleepTimer = false })
    }
}
