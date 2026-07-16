package com.lyd.player.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

const val RECENT_PLAYS_CAP = 20

@Dao
interface RecentPlayDao {

    @Query("SELECT * FROM recent_plays ORDER BY playedAt DESC LIMIT $RECENT_PLAYS_CAP")
    fun observeRecentPlays(): Flow<List<RecentPlayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentPlayEntity)

    @Query(
        "DELETE FROM recent_plays WHERE songPath NOT IN " +
            "(SELECT songPath FROM recent_plays ORDER BY playedAt DESC LIMIT $RECENT_PLAYS_CAP)",
    )
    suspend fun trim()

    @Transaction
    suspend fun record(songPath: String, playedAt: Long) {
        upsert(RecentPlayEntity(songPath, playedAt))
        trim()
    }
}
