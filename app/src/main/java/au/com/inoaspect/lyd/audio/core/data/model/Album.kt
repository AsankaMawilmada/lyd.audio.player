package au.com.inoaspect.lyd.audio.core.data.model

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val artistId: Long,
    val songCount: Int,
    val totalDuration: Long,
)

data class Artist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
)

data class Folder(
    val path: String,
    val name: String,
    val trackCount: Int,
    val totalDuration: Long,
)
