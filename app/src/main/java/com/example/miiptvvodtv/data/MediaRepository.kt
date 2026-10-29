package com.example.miiptvvodtv.data

import android.content.Context
import android.util.Log
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.miiptvvodtv.ApiService
import com.example.miiptvvodtv.Channel
import com.example.miiptvvodtv.MediaResponse
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

data class FetchResult(
    val data: MediaResponse,
    val isFromCache: Boolean
)

data class CachedMediaWrapper(
    val timestamp: Long,
    val data: MediaResponse
)

class MediaRepository(private val context: Context) {

    private val gson = Gson()
    private val cacheFile: File by lazy {
        File(context.cacheDir, "media_cache_v2.json")
    }

    // Obtener caché local instantánea (si existe) para apertura ultrarrápida (Stale-First)
    fun getCachedContent(): MediaResponse? {
        return try {
            if (cacheFile.exists()) {
                val jsonString = cacheFile.readText()
                val wrapper = gson.fromJson(jsonString, CachedMediaWrapper::class.java)
                wrapper?.data
            } else {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val oldJson = prefs.getString(KEY_CACHE_JSON, null)
                if (!oldJson.isNullOrEmpty()) {
                    gson.fromJson(oldJson, MediaResponse::class.java)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error al leer datos de la caché local", e)
            null
        }
    }

    // Refrescar contenido desde la red (Soporta tanto JSON como listas M3U/M3U8 con M3UParser)
    suspend fun fetchAndSaveRemoteContent(
        onProgress: (percent: Int, status: String) -> Unit = { _, _ -> }
    ): MediaResponse = withContext(Dispatchers.IO) {
        onProgress(10, "Actualizando desde la nube...")

        val rawText = try {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/Luchitxo/listas-iptv/refs/heads/main/tv_vod.json")
                .build()
            client.newCall(request).execute().use { response ->
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error descargando contenido crudo, intentando fallback Retrofit", e)
            val retrofitRes = ApiService.instance.getContent()
            return@withContext processMediaResponse(retrofitRes, onProgress)
        }

        val channels = if (rawText.trim().startsWith("#EXTM3U", ignoreCase = true) || rawText.contains("#EXTINF:")) {
            M3UParser.parse(rawText)
        } else {
            val resp = gson.fromJson(rawText, MediaResponse::class.java)
            resp?.channels ?: emptyList()
        }

        processChannels(channels, onProgress)
    }

    private fun processMediaResponse(
        response: MediaResponse,
        onProgress: (percent: Int, status: String) -> Unit
    ): MediaResponse {
        return processChannels(response.channels ?: emptyList(), onProgress)
    }

    private fun processChannels(
        rawChannels: List<Channel>,
        onProgress: (percent: Int, status: String) -> Unit
    ): MediaResponse {
        if (rawChannels.isEmpty()) {
            throw IllegalStateException("El servidor devolvió una lista vacía de canales.")
        }

        onProgress(50, "Validando y filtrando canales...")
        val validChannels = rawChannels.filter {
            !it.id.isBlank() && !it.url.isBlank()
        }.map { channel ->
            channel.copy(
                name = if (channel.name.isBlank()) "Canal sin nombre" else channel.name,
                category = if (channel.category.isBlank()) "General" else channel.category
            )
        }.distinctBy { it.id }

        if (validChannels.isEmpty()) {
            throw IllegalStateException("No hay canales válidos tras la validación.")
        }

        val sanitizedResponse = MediaResponse(channels = validChannels)

        onProgress(80, "Guardando caché en archivo...")
        val wrapper = CachedMediaWrapper(
            timestamp = System.currentTimeMillis(),
            data = sanitizedResponse
        )
        cacheFile.writeText(gson.toJson(wrapper))

        // Precarga inteligente en segundo plano
        preloadImages(validChannels, getFavorites())

        onProgress(100, "¡Actualizado con éxito!")
        return sanitizedResponse
    }

    // Método combinado para compatibilidad (Stale-While-Revalidate)
    suspend fun getMediaContent(
        onProgress: (percent: Int, status: String) -> Unit = { _, _ -> }
    ): FetchResult = withContext(Dispatchers.IO) {
        val cached = getCachedContent()
        if (cached != null && !cached.channels.isNullOrEmpty()) {
            FetchResult(data = cached, isFromCache = true)
        } else {
            val freshData = fetchAndSaveRemoteContent(onProgress)
            FetchResult(data = freshData, isFromCache = false)
        }
    }

    private fun preloadImages(channels: List<Channel>, favoriteIds: Set<String>) {
        val imageLoader = context.imageLoader
        val favoriteChannels = channels.filter { favoriteIds.contains(it.id) }
        val otherChannels = channels.filter { !favoriteIds.contains(it.id) }
        val channelsToPreload = (favoriteChannels + otherChannels).distinctBy { it.id }.take(50)

        channelsToPreload.forEach { channel ->
            if (channel.logo.isNotEmpty()) {
                val request = ImageRequest.Builder(context)
                    .data(channel.logo)
                    .size(160, 160)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    fun getFavorites(): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(id: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentFavorites = getFavorites().toMutableSet()
        if (currentFavorites.contains(id)) {
            currentFavorites.remove(id)
        } else {
            currentFavorites.add(id)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, currentFavorites).apply()
        return currentFavorites
    }

    fun getPreferredUrl(channelId: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("preferred_url_$channelId", null)
    }

    fun savePreferredUrl(channelId: String, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("preferred_url_$channelId", url).apply()
    }

    fun clearPreferredUrl(channelId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("preferred_url_$channelId").apply()
    }

    companion object {
        private const val TAG = "MediaRepository"
        private const val PREFS_NAME = "mi_iptv_vod_prefs"
        private const val KEY_CACHE_JSON = "cached_media_json"
        private const val KEY_FAVORITES = "favorite_ids"
    }
}
