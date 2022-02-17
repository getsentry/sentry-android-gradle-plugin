package io.sentry.android.instrumentation.lib.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoritesDao {

    @Query("SELECT * FROM Favorite ORDER BY FavoriteId DESC")
    abstract fun all(): Flow<List<Favorite>>

    @Transaction
    @Query("DELETE FROM Favorite")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(favorite: Favorite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(vararg favorites: Favorite)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(favorite: Favorite)

    @Transaction
    @Delete
    abstract suspend fun delete(favorite: Favorite)
}
