package au.com.inoaspect.lyd.audio.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArtCacheDao {

    @Query("SELECT * FROM art_cache WHERE albumId = :albumId")
    suspend fun getEntry(albumId: Long): ArtCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ArtCacheEntity)
}
