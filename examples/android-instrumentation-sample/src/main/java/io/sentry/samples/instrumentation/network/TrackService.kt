package io.sentry.samples.instrumentation.network

import io.sentry.samples.instrumentation.data.Track
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path

interface TrackService {

    @GET("v3/{uuid}")
    suspend fun tracks(@Path("uuid") uuid: String): List<Track>

    companion object {
        private val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val instance = retrofit.create<TrackService>()
    }
}
