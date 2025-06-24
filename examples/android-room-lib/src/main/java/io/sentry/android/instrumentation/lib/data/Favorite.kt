package io.sentry.android.instrumentation.lib.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// not adding a foreignKeys to TrackId because it'd cause a circle dependency.
// the goal is just that auto instrumentation works for transitive dependencies,
// so the FavoritesDao methods get instrumented too.
@Entity(tableName = "Favorite")
data class Favorite(@PrimaryKey @ColumnInfo(name = "FavoriteId") val id: Long)
