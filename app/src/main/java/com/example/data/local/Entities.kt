package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val isLocalFile: Boolean = false,
    val channelCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val isCustom: Boolean = false
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
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
)
