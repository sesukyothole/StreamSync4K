package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistEntity
import com.example.data.model.Channel
import com.example.data.model.Playlist
import com.example.data.parser.M3uParser
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

class StreamRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context)
) {
    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { entities ->
        entities.map { it.toModel() }
    }

    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels().map { entities ->
        entities.map { it.toModel() }
    }

    fun getChannelsForPlaylist(playlistId: Long): Flow<List<Channel>> {
        return if (playlistId == 0L) {
            allChannels
        } else {
            channelDao.getChannelsForPlaylist(playlistId).map { entities ->
                entities.map { it.toModel() }
            }
        }
    }

    val favoriteChannels: Flow<List<Channel>> = channelDao.getFavoriteChannels().map { entities ->
        entities.map { it.toModel() }
    }

    fun normalizeUrl(rawUrl: String): String = M3uParser.normalizeUrl(rawUrl)

    // Cache-first initialization: reads from Room cache if cached with new URLs.
    // Automatically seeds and caches all 3 new M3U sources: adult.m3u, adult2.m3u, and 0108.m3u
    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        val p1 = playlistDao.getPlaylistById(1L)
        val isUpdatedSource = p1?.sourceUrl == SampleData.URL_ADULT_1
        val c1Count = if (p1 != null) channelDao.getChannelCount(1L) else 0

        if (!isUpdatedSource || c1Count < 50) {
            // Clear any old/test channels completely
            channelDao.deleteAllChannels()
            for (source in SampleData.DEFAULT_SOURCES) {
                playlistDao.insertPlaylist(source.toEntity())
            }

            // Fetch and cache the primary source (PL1)
            try {
                fetchAndSaveOnlinePlaylist(1L, SampleData.URL_ADULT_1, "PL1")
            } catch (_: Exception) {}

            // Also cache PL2 and PL3
            try {
                fetchAndSaveOnlinePlaylist(2L, SampleData.URL_ADULT_2, "PL2")
            } catch (_: Exception) {}

            try {
                fetchAndSaveOnlinePlaylist(3L, SampleData.URL_0108, "PL3")
            } catch (_: Exception) {}
        }
    }

    suspend fun refreshAllSourcesFresh(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        channelDao.deleteAllChannels()
        var totalAdded = 0
        var totalCount = 0
        for (source in SampleData.DEFAULT_SOURCES) {
            try {
                val (added, count) = fetchAndSaveOnlinePlaylist(source.id, source.sourceUrl, source.title)
                totalAdded += added
                totalCount += count
            } catch (_: Exception) {}
        }
        Pair(totalAdded, totalCount)
    }

    // Triggered when user presses the Reload / Refresh button, or during initial empty cache setup
    suspend fun fetchAndSaveOnlinePlaylist(playlistId: Long, url: String, title: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url)
        val request = Request.Builder()
            .url(normalizedUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("Gagal memuat URL playlist: HTTP ${response.code}")
        }

        val bodyString = response.body?.string() ?: throw IllegalStateException("Respons kosong dari server")
        val newChannels = M3uParser.parse(bodyString, playlistId)

        if (newChannels.isNotEmpty()) {
            val existingChannels = channelDao.getChannelsListForPlaylist(playlistId)
            val existingUrls = existingChannels.map { it.streamUrl }.toSet()
            val newAddedCount = newChannels.count { it.streamUrl !in existingUrls }

            channelDao.deleteChannelsForPlaylist(playlistId)
            newChannels.chunked(500).forEach { batch ->
                channelDao.insertChannels(batch.map { it.toEntity() })
            }

            val updatedPlaylist = PlaylistEntity(
                id = playlistId,
                title = title,
                sourceUrl = normalizedUrl,
                isLocalFile = false,
                channelCount = newChannels.size,
                createdAt = System.currentTimeMillis(),
                isDefault = playlistId == 1L,
                isCustom = false
            )
            playlistDao.updatePlaylist(updatedPlaylist)

            // Notify user of new or updated content
            NotificationHelper.sendPlaylistUpdateNotification(
                context = context,
                playlistTitle = title,
                addedCount = newAddedCount,
                totalCount = newChannels.size
            )

            return@withContext Pair(newAddedCount, newChannels.size)
        }
        Pair(0, 0)
    }

    suspend fun updatePlaylistSource(playlistId: Long, newTitle: String, newUrl: String) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val normalized = normalizeUrl(newUrl)
        val updated = existing.copy(title = newTitle, sourceUrl = normalized)
        playlistDao.updatePlaylist(updated)
    }

    suspend fun addNewOnlinePlaylist(title: String, url: String): Long = withContext(Dispatchers.IO) {
        val normalized = normalizeUrl(url)
        val entity = PlaylistEntity(
            title = title,
            sourceUrl = normalized,
            isLocalFile = false,
            channelCount = 0,
            isDefault = false,
            isCustom = false
        )
        val newId = playlistDao.insertPlaylist(entity)
        try {
            fetchAndSaveOnlinePlaylist(newId, url, title)
        } catch (_: Exception) {
            // Saved locally so user can reload later
        }
        newId
    }

    suspend fun createCustomPlaylist(title: String): Long = withContext(Dispatchers.IO) {
        val entity = PlaylistEntity(
            title = title,
            sourceUrl = "custom",
            isLocalFile = true,
            channelCount = 0,
            isDefault = false,
            isCustom = true
        )
        playlistDao.insertPlaylist(entity)
    }

    suspend fun addChannelToPlaylist(targetPlaylistId: Long, channel: Channel) = withContext(Dispatchers.IO) {
        val newChannel = channel.copy(
            id = "${targetPlaylistId}_custom_${System.currentTimeMillis()}_${channel.name.hashCode()}",
            playlistId = targetPlaylistId,
            addedAt = System.currentTimeMillis()
        )
        channelDao.insertChannel(newChannel.toEntity())
        playlistDao.updateChannelCount(targetPlaylistId)
    }

    suspend fun deleteChannel(channelId: String, playlistId: Long) = withContext(Dispatchers.IO) {
        channelDao.deleteChannel(channelId)
        playlistDao.updateChannelCount(playlistId)
    }

    suspend fun importLocalM3uFile(title: String, uri: Uri): Long = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Tidak dapat membaca berkas yang dipilih")

        val content = inputStream.bufferedReader().use(BufferedReader::readText)
        val entity = PlaylistEntity(
            title = title,
            sourceUrl = uri.toString(),
            isLocalFile = true,
            channelCount = 0,
            isDefault = false,
            isCustom = false
        )
        val newId = playlistDao.insertPlaylist(entity)
        val channels = M3uParser.parse(content, newId)
        if (channels.isNotEmpty()) {
            channelDao.insertChannels(channels.map { it.toEntity() })
            playlistDao.updatePlaylist(entity.copy(id = newId, channelCount = channels.size))
        }
        newId
    }

    suspend fun addDirectChannel(channel: Channel) = withContext(Dispatchers.IO) {
        channelDao.insertChannels(listOf(channel.toEntity()))
        playlistDao.updateChannelCount(channel.playlistId)
    }

    suspend fun addDirectStreamChannel(playlistId: Long, name: String, streamUrl: String, groupTitle: String): String = withContext(Dispatchers.IO) {
        val is4k = name.contains("4K", ignoreCase = true) || streamUrl.contains("4k", ignoreCase = true)
        val channel = Channel(
            id = "${playlistId}_manual_${System.currentTimeMillis()}",
            playlistId = playlistId,
            name = name,
            streamUrl = streamUrl,
            groupTitle = groupTitle.ifBlank { "Custom" },
            resolutionLabel = if (is4k) "4K UHD" else "HD/Auto",
            is4k = is4k,
            addedAt = System.currentTimeMillis()
        )
        channelDao.insertChannels(listOf(channel.toEntity()))
        playlistDao.updateChannelCount(playlistId)
        channel.id
    }

    suspend fun setFavorite(channelId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        channelDao.setFavorite(channelId, isFavorite)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        channelDao.deleteChannelsForPlaylist(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    private fun PlaylistEntity.toModel(): Playlist {
        return Playlist(
            id = id,
            title = title,
            sourceUrl = sourceUrl,
            isLocalFile = isLocalFile,
            channelCount = channelCount,
            createdAt = createdAt,
            isDefault = isDefault,
            isCustom = isCustom
        )
    }

    private fun Playlist.toEntity(): PlaylistEntity {
        return PlaylistEntity(
            id = id,
            title = title,
            sourceUrl = sourceUrl,
            isLocalFile = isLocalFile,
            channelCount = channelCount,
            createdAt = createdAt,
            isDefault = isDefault,
            isCustom = isCustom
        )
    }

    private fun ChannelEntity.toModel(): Channel {
        return Channel(
            id = id,
            playlistId = playlistId,
            name = name,
            streamUrl = streamUrl,
            logoUrl = logoUrl,
            groupTitle = groupTitle,
            userAgent = userAgent,
            licenseKey = licenseKey,
            licenseType = licenseType,
            isFavorite = isFavorite,
            resolutionLabel = resolutionLabel,
            is4k = is4k,
            orderIndex = orderIndex,
            addedAt = addedAt
        )
    }

    private fun Channel.toEntity(): ChannelEntity {
        return ChannelEntity(
            id = id,
            playlistId = playlistId,
            name = name,
            streamUrl = streamUrl,
            logoUrl = logoUrl,
            groupTitle = groupTitle,
            userAgent = userAgent,
            licenseKey = licenseKey,
            licenseType = licenseType,
            isFavorite = isFavorite,
            resolutionLabel = resolutionLabel,
            is4k = is4k,
            orderIndex = orderIndex,
            addedAt = addedAt
        )
    }
}
