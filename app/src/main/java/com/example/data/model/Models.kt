package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Channel(
    val id: String,
    val playlistId: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "General",
    val userAgent: String? = null,
    val licenseKey: String? = null,
    val licenseType: String? = null,
    val isFavorite: Boolean = false,
    val resolutionLabel: String = "HD/Auto",
    val is4k: Boolean = false,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
) {
    val fileName: String
        get() = streamUrl.substringBefore("?").substringBefore("#").substringAfterLast("/").ifBlank { "stream" }
}

@Immutable
data class Playlist(
    val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val isLocalFile: Boolean = false,
    val channelCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val isCustom: Boolean = false
)

enum class ChannelSortOrder(val label: String) {
    ORIGINAL("Urutan Asli Sumber"),
    NAME_ASC("Nama (A - Z)"),
    NAME_DESC("Nama (Z - A)"),
    DATE_DESC("Terbaru Ditambahkan"),
    DATE_ASC("Terlama Ditambahkan")
}

@Immutable
data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

@Immutable
data class SubtitleTrackInfo(
    val id: String,
    val language: String,
    val label: String,
    val isExternal: Boolean = false,
    val uri: String? = null
)

@Immutable
data class VideoTrackInfo(
    val id: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val label: String,
    val isSelected: Boolean = false
)

enum class AspectRatioMode(val label: String) {
    FIT("Sesuaikan (Fit)"),
    FILL("Layar Penuh (Fill)"),
    ZOOM("Perbesar (Zoom)"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

data class SubtitleSettings(
    val isEnabled: Boolean = true,
    val offsetMs: Long = 0L,
    val fontSizeSp: Float = 18f,
    val textColorHex: Long = 0xFFFFFFFF,
    val hasBackgroundBox: Boolean = true
)

data class PlaylistUpdateNotification(
    val id: String = "${System.currentTimeMillis()}",
    val playlistTitle: String,
    val addedCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
