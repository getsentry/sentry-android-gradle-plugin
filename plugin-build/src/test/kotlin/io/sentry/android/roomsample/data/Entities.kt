package io.sentry.android.roomsample.data

// these need to be on the classpath, so the ASM verifier can resolve superclasses and control flows properly
data class Track(
    val id: Long = 0,
    val name: String,
    val albumId: Long?,
    val composer: String?,
    val mediaTypeId: Long?,
    val genreId: Long?,
    val millis: Long,
    val bytes: Long?,
    val price: Float
)

open class Album(
    val id: Long,
    val title: String,
    val artistId: Long
)

data class MultiPKeyEntity(
    val id: Long,
    val name: String
)

class SubAlbum(
    id: Long,
    title: String,
    artistId: Long
) : Album(id, title, artistId)

data class NameWithComposer(val id: Int, val nameWithComposer: String)
