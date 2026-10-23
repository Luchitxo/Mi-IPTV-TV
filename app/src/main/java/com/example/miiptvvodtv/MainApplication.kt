package com.example.miiptvvodtv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MainApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30) // 30% de memoria RAM reservada para caché de imágenes
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150 * 1024 * 1024) // 150 MB de caché en disco
                    .build()
            }
            .respectCacheHeaders(false) // Forzar caché local en disco para logotipos de TV
            .allowHardware(true) // Decodificación acelerada por hardware GPU
            .build()
    }
}
