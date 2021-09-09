package io.sentry.android.roomsample.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TracksDao {

    @Query("SELECT * FROM track ORDER BY TrackId DESC")
    abstract fun all(): Flow<List<Track>>

    @Query("SELECT * FROM Track WHERE AlbumId = (SELECT AlbumId FROM Album WHERE ArtistId = (SELECT ArtistId from Artist WHERE Name = :bandName))")
    abstract suspend fun allByArtist(bandName: String): List<Track>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(track: Track)
}
