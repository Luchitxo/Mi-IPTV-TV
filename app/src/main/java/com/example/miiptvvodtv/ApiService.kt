package com.example.miiptvvodtv

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

data class MediaResponse(
    val channels: List<Channel>?
)

interface ApiService {
    @GET("Luchitxo/listas-iptv/refs/heads/main/tv_vod.json")
    suspend fun getContent(): MediaResponse

    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/"

        // Instancia Singleton optimizada con OkHttp y timeouts explícitos
        val instance: ApiService by lazy {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
