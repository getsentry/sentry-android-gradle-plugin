package io.sentry.android.roomsample.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "Artist")
data class Artist(
    @PrimaryKey @ColumnInfo(name = "ArtistId") val id: Long,
    @ColumnInfo(name = "Name") val name: String?,
)

@Entity(
    tableName = "Album",
    foreignKeys = [ForeignKey(
        entity = Artist::class,
        parentColumns = arrayOf("ArtistId"),
        childColumns = arrayOf("ArtistId")
    )]
)
data class Album(
    @PrimaryKey @ColumnInfo(name = "AlbumId") val id: Long,
    @ColumnInfo(name = "Title") val title: String,
    @ColumnInfo(name = "ArtistId") val artistId: Long
)

@Entity(
    tableName = "Track",
    foreignKeys = [ForeignKey(
        entity = Album::class,
        parentColumns = ["AlbumId"],
        childColumns = ["AlbumId"]
    )]
)
data class Track(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "TrackId") val id: Long = 0,
    @ColumnInfo(name = "Name") val name: String,
    @ColumnInfo(name = "AlbumId") val albumId: Long?,
    @ColumnInfo(name = "Composer") val composer: String?,
    @ColumnInfo(name = "MediaTypeId") val mediaTypeId: Long?,
    @ColumnInfo(name = "GenreId") val genreId: Long?,
    @ColumnInfo(name = "Milliseconds") val millis: Long,
    @ColumnInfo(name = "Bytes") val bytes: Long?,
    @ColumnInfo(name = "UnitPrice") val price: Float
)
