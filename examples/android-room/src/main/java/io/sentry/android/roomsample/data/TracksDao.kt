package io.sentry.android.roomsample.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TracksDao {

    @Query("SELECT * FROM track ORDER BY TrackId DESC")
    abstract fun all(): Flow<List<Track>>

    @Query(
        "SELECT * FROM Track WHERE AlbumId = (SELECT AlbumId FROM Album WHERE ArtistId = " +
            "(SELECT ArtistId from Artist WHERE Name = :bandName))"
    )
    abstract fun allByArtist(bandName: String): List<Track>

    @Transaction
    @Query("DELETE FROM Track")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(track: Track): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(vararg tracks: Track)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(track: Track)

    @Transaction
    @Delete
    abstract suspend fun delete(track: Track)
}
