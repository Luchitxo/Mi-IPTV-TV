package com.example.miiptvvodtv.data

import com.example.miiptvvodtv.Channel
import java.util.UUID

object M3UParser {

    fun parse(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = m3uContent.lines()

        var currentName = "Canal sin nombre"
        var currentCategory = "General"
        var currentLogo = ""
        var currentId = ""
        var currentHeaders = mutableMapOf<String, String>()
        var pendingExtinf = false

        val extinfRegex = Regex("""#EXTINF:(?:-?\d+)?\s*(.*)""", RegexOption.IGNORE_CASE)
        val attrRegex = Regex("""([a-zA-Z0-9_-]+)=["']?([^"']*)["']?""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("#EXTM3U", ignoreCase = true) -> {
                    // Header M3U
                }
                trimmed.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingExtinf = true
                    currentName = "Canal sin nombre"
                    currentCategory = "General"
                    currentLogo = ""
                    currentId = ""
                    currentHeaders.clear()

                    val match = extinfRegex.find(trimmed)
                    if (match != null) {
                        val paramsAndTitle = match.groupValues[1]

                        // Extraer atributos mediante regex tolerante a comillas y espacios
                        attrRegex.findAll(paramsAndTitle).forEach { attrMatch ->
                            val key = attrMatch.groupValues[1].lowercase()
                            val value = attrMatch.groupValues[2]
                            when {
                                key.contains("tvg-id") -> currentId = value
                                key.contains("tvg-name") && currentName == "Canal sin nombre" -> currentName = value
                                key.contains("tvg-logo") -> currentLogo = value
                                key.contains("group-title") -> currentCategory = value
                            }
                        }

                        // El nombre del canal está después de la última coma
                        val commaIndex = paramsAndTitle.lastIndexOf(',')
                        if (commaIndex != -1 && commaIndex < paramsAndTitle.length - 1) {
                            val title = paramsAndTitle.substring(commaIndex + 1).trim()
                            if (title.isNotEmpty()) {
                                currentName = title
                            }
                        }
                    }
                }
                trimmed.startsWith("#EXTVLCOPT", ignoreCase = true) -> {
                    // Soporte para cabeceras personalizadas #EXTVLCOPT:http-user-agent=... o http-referer=...
                    val optContent = trimmed.substringAfter("#EXTVLCOPT:", "").trim()
                    if (optContent.contains("http-user-agent=", ignoreCase = true)) {
                        val ua = optContent.substringAfter("http-user-agent=", "").trim()
                        if (ua.isNotEmpty()) {
                            currentHeaders["User-Agent"] = ua
                        }
                    } else if (optContent.contains("http-referer=", ignoreCase = true) || optContent.contains("http-referrer=", ignoreCase = true)) {
                        val ref = optContent.substringAfter("http-referer=", "").let { if (it == optContent) optContent.substringAfter("http-referrer=", "") else it }.trim()
                        if (ref.isNotEmpty()) {
                            currentHeaders["Referer"] = ref
                        }
                    }
                }
                !trimmed.startsWith("#") && pendingExtinf -> {
                    // Línea con la URL de streaming del canal
                    val url = trimmed
                    val finalId = if (currentId.isNotBlank()) currentId else UUID.nameUUIDFromBytes(url.toByteArray()).toString()

                    channels.add(
                        Channel(
                            id = finalId,
                            name = currentName,
                            category = currentCategory.ifEmpty { "General" },
                            logo = currentLogo,
                            url = url,
                            headers = if (currentHeaders.isNotEmpty()) currentHeaders.toMap() else null
                        )
                    )
                    pendingExtinf = false
                }
            }
        }

        return channels
    }
}
