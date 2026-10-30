package com.example.data.parser

import com.example.data.model.Channel
import java.util.regex.Pattern

object M3uParser {

    private val TVG_ID_REGEX = Pattern.compile("tvg-id=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val TVG_NAME_REGEX = Pattern.compile("tvg-name=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val TVG_LOGO_REGEX = Pattern.compile("tvg-logo=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val GROUP_TITLE_REGEX = Pattern.compile("group-title=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val USER_AGENT_REGEX = Pattern.compile("http-user-agent=([^\\s,]+)", Pattern.CASE_INSENSITIVE)
    private val LICENSE_KEY_REGEX = Pattern.compile("license_key=([^\\s,]+)", Pattern.CASE_INSENSITIVE)
    private val LICENSE_TYPE_REGEX = Pattern.compile("license_type=([^\\s,]+)", Pattern.CASE_INSENSITIVE)

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return if (trimmed.contains("github.com") && trimmed.contains("/blob/")) {
            trimmed.replace("https://github.com/", "https://raw.githubusercontent.com/")
                .replace("http://github.com/", "https://raw.githubusercontent.com/")
                .replace("/blob/", "/")
        } else {
            trimmed
        }
    }

    fun parse(content: String, playlistId: Long): List<Channel> {
        val lines = content.lines()
        val channels = mutableListOf<Channel>()

        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup = "General"
        var currentUserAgent: String? = null
        var currentLicenseKey: String? = null
        var currentLicenseType: String? = null
        var index = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse attributes
                val tvgName = extractMatch(TVG_NAME_REGEX, line)
                currentLogo = extractMatch(TVG_LOGO_REGEX, line)
                val group = extractMatch(GROUP_TITLE_REGEX, line)
                if (!group.isNullOrBlank()) {
                    currentGroup = group
                }

                // Channel name is after the last comma
                val commaIndex = line.lastIndexOf(',')
                currentName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                    line.substring(commaIndex + 1).trim()
                } else if (!tvgName.isNullOrBlank()) {
                    tvgName
                } else {
                    "Channel ${index + 1}"
                }
            } else if (line.startsWith("#EXTVLCOPT:", ignoreCase = true)) {
                val ua = extractMatch(USER_AGENT_REGEX, line)
                if (!ua.isNullOrBlank()) currentUserAgent = ua
            } else if (line.startsWith("#KODIPROP:", ignoreCase = true)) {
                val key = extractMatch(LICENSE_KEY_REGEX, line)
                if (!key.isNullOrBlank()) currentLicenseKey = key
                val type = extractMatch(LICENSE_TYPE_REGEX, line)
                if (!type.isNullOrBlank()) currentLicenseType = type
            } else if (!line.startsWith("#")) {
                // This is the stream URL
                if (line.startsWith("http://") || line.startsWith("https://") ||
                    line.startsWith("rtsp://") || line.startsWith("rtmp://") ||
                    line.startsWith("file://") || line.startsWith("content://") ||
                    line.contains("://") || line.startsWith("/")
                ) {
                    val name = currentName ?: "Stream ${index + 1}"
                    val is4k = name.contains("4K", ignoreCase = true) ||
                            name.contains("UHD", ignoreCase = true) ||
                            name.contains("2160", ignoreCase = true) ||
                            line.contains("4k", ignoreCase = true)

                    val isMpd = line.contains(".mpd", ignoreCase = true) ||
                            line.contains("/mpd", ignoreCase = true) ||
                            line.contains("dash", ignoreCase = true) ||
                            currentLicenseType?.contains("clearkey", ignoreCase = true) == true

                    val resolutionLabel = when {
                        is4k -> "4K UHD"
                        name.contains("FHD", ignoreCase = true) || name.contains("1080", ignoreCase = true) -> "1080p FHD"
                        name.contains("HD", ignoreCase = true) || name.contains("720", ignoreCase = true) -> "720p HD"
                        isMpd -> "DASH MPD"
                        else -> "HD/Auto"
                    }

                    channels.add(
                        Channel(
                            id = "${playlistId}_${index}_${line.hashCode()}",
                            playlistId = playlistId,
                            name = name,
                            streamUrl = line,
                            logoUrl = currentLogo?.takeIf { it.isNotBlank() },
                            groupTitle = currentGroup.ifBlank { "General" },
                            userAgent = currentUserAgent,
                            licenseKey = currentLicenseKey,
                            licenseType = currentLicenseType,
                            isFavorite = false,
                            resolutionLabel = resolutionLabel,
                            is4k = is4k,
                            orderIndex = index
                        )
                    )

                    // Reset for next entry
                    currentName = null
                    currentLogo = null
                    currentGroup = "General"
                    currentUserAgent = null
                    currentLicenseKey = null
                    currentLicenseType = null
                    index++
                }
            }
        }

        return channels
    }

    private fun extractMatch(pattern: Pattern, input: String): String? {
        val matcher = pattern.matcher(input)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }
}
