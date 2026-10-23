package com.example.miiptvvodtv

import androidx.compose.runtime.Immutable

object FilterConstants {
    const val ALL = "Todas"
    const val FAVORITES = "Favoritos"
}

@Immutable
data class ChannelAlternative(
    val url: String = "",
    val headers: Map<String, String>? = null
)

// Modelo tolerante con valores por defecto para evitar NullPointerException en Gson
@Immutable
data class Channel(
    val id: String = "",
    val name: String = "Canal sin nombre",
    val category: String = "General",
    val logo: String = "",
    val url: String = "",
    val headers: Map<String, String>? = null,
    val alternatives: List<ChannelAlternative>? = null
)
