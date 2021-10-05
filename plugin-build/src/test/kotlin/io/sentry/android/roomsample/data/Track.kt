package io.sentry.android.roomsample.data

// this needs to be on the classpath, so the ASN verifier can resolve superclasses properly
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
