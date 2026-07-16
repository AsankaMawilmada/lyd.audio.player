package com.lyd.player.nav

import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val LIBRARY_TAB_ARG = "tab"
    const val LIBRARY = "library?$LIBRARY_TAB_ARG={$LIBRARY_TAB_ARG}"
    const val PLAYLISTS = "playlists"
    const val SEARCH = "search"

    fun library(tab: String = "songs") = "library?$LIBRARY_TAB_ARG=$tab"

    const val NOW_PLAYING = "now_playing"
    const val EQUALIZER = "equalizer"
    const val CREATE_PLAYLIST = "create_playlist"

    const val ALBUM_ID_ARG = "albumId"
    const val ARTIST_ID_ARG = "artistId"
    const val FOLDER_PATH_ARG = "folderPath"
    const val PLAYLIST_ID_ARG = "playlistId"

    const val ALBUM_DETAIL = "album/{$ALBUM_ID_ARG}"
    const val ARTIST_DETAIL = "artist/{$ARTIST_ID_ARG}"
    const val FOLDER_DETAIL = "folder/{$FOLDER_PATH_ARG}"
    const val PLAYLIST_DETAIL = "playlist/{$PLAYLIST_ID_ARG}"

    fun albumDetail(albumId: Long) = "album/$albumId"
    fun artistDetail(artistId: Long) = "artist/$artistId"
    fun folderDetail(path: String) = "folder/${URLEncoder.encode(path, "UTF-8")}"
    fun playlistDetail(playlistId: Long) = "playlist/$playlistId"

    fun decodeFolderPath(encoded: String): String = URLDecoder.decode(encoded, "UTF-8")
}

val TOP_LEVEL_ROUTES = setOf(Routes.HOME, Routes.LIBRARY, Routes.PLAYLISTS, Routes.SEARCH)
