package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Channel
import com.example.data.model.ChannelSortOrder
import com.example.data.model.Playlist
import com.example.data.model.PlaylistUpdateNotification
import com.example.data.repository.StreamRepository
import com.example.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StreamRepository(application)
    val playerManager = PlayerManager(application, viewModelScope)

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlaylistId = MutableStateFlow<Long>(0L) // 0L: Semua Sumber (Gabungan)
    val selectedPlaylistId: StateFlow<Long> = _selectedPlaylistId.asStateFlow()

    val favoriteChannels: StateFlow<List<Channel>> = repository.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow("Semua")
    val selectedGroup: StateFlow<String> = _selectedGroup.asStateFlow()

    private val _sortOrder = MutableStateFlow(ChannelSortOrder.ORIGINAL)
    val sortOrder: StateFlow<ChannelSortOrder> = _sortOrder.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isPlayerOpen = MutableStateFlow(false)
    val isPlayerOpen: StateFlow<Boolean> = _isPlayerOpen.asStateFlow()

    private val _selectedPreviewChannel = MutableStateFlow<Channel?>(null)
    val selectedPreviewChannel: StateFlow<Channel?> = _selectedPreviewChannel.asStateFlow()

    // Dialogs
    private val _playlistToEdit = MutableStateFlow<Playlist?>(null)
    val playlistToEdit: StateFlow<Playlist?> = _playlistToEdit.asStateFlow()

    private val _deviceSpecSummary = MutableStateFlow("Memeriksa spek device...")
    val deviceSpecSummary: StateFlow<String> = _deviceSpecSummary.asStateFlow()

    private val _showChannelPopup = MutableStateFlow(true)
    val showChannelPopup: StateFlow<Boolean> = _showChannelPopup.asStateFlow()

    fun setShowChannelPopup(show: Boolean) {
        _showChannelPopup.value = show
    }

    private val _recentNotifications = MutableStateFlow<List<PlaylistUpdateNotification>>(emptyList())
    val recentNotifications: StateFlow<List<PlaylistUpdateNotification>> = _recentNotifications.asStateFlow()

    // Selected playlist raw channels (loaded from local Room cache)
    val currentChannels: StateFlow<List<Channel>> = _selectedPlaylistId
        .flatMapLatest { playlistId -> repository.getChannelsForPlaylist(playlistId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered by Search (Title or File Name), Group, and Sorted
    val filteredChannels: StateFlow<List<Channel>> = combine(
        currentChannels,
        _searchQuery,
        _selectedGroup,
        _sortOrder
    ) { channels, query, group, sort ->
        val filtered = channels.filter { channel ->
            val matchesQuery = query.isBlank() ||
                    channel.name.contains(query, ignoreCase = true) ||
                    channel.fileName.contains(query, ignoreCase = true) ||
                    channel.streamUrl.contains(query, ignoreCase = true) ||
                    channel.groupTitle.contains(query, ignoreCase = true)
            val matchesGroup = group == "Semua" ||
                    (group == "4K UHD" && channel.is4k) ||
                    channel.groupTitle.equals(group, ignoreCase = true)
            matchesQuery && matchesGroup
        }

        when (sort) {
            ChannelSortOrder.ORIGINAL -> filtered.sortedBy { it.orderIndex }
            ChannelSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            ChannelSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            ChannelSortOrder.DATE_DESC -> filtered.sortedByDescending { it.addedAt }
            ChannelSortOrder.DATE_ASC -> filtered.sortedBy { it.addedAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique groups for filter chips
    val availableGroups: StateFlow<List<String>> = currentChannels.combine(MutableStateFlow(Unit)) { channels, _ ->
        val groups = channels.map { it.groupTitle }.distinct().filter { it.isNotBlank() }.sorted()
        val has4k = channels.any { it.is4k }
        val result = mutableListOf("Semua")
        if (has4k) result.add("4K UHD")
        result.addAll(groups)
        result.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Semua"))

    init {
        val maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val processors = Runtime.getRuntime().availableProcessors()
        _deviceSpecSummary.value = "RAM: ~${maxMemoryMB}MB | Cores: $processors | Model: ${android.os.Build.MODEL} (85% Live View Optimized)"

        // Cache-first: Room loads cached channels immediately
        viewModelScope.launch(Dispatchers.IO) {
            repository.initializeDefaultsIfNeeded()
        }
    }

    fun selectPlaylist(playlistId: Long) {
        _selectedPlaylistId.value = playlistId
        _selectedGroup.value = "Semua"
        _searchQuery.value = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGroup(group: String) {
        _selectedGroup.value = group
    }

    fun setSortOrder(order: ChannelSortOrder) {
        _sortOrder.value = order
    }

    fun playChannel(channel: Channel) {
        _selectedPreviewChannel.value = channel
        playerManager.playChannel(channel)
        _isPlayerOpen.value = true
    }

    fun selectChannelForPreview(channel: Channel) {
        _selectedPreviewChannel.value = channel
        playerManager.playChannel(channel)
    }

    fun playChannelFullScreen(channel: Channel) {
        _selectedPreviewChannel.value = channel
        if (playerManager.currentChannel.value?.id != channel.id) {
            playerManager.playChannel(channel)
        }
        _isPlayerOpen.value = true
    }

    fun openFullScreen() {
        _isPlayerOpen.value = true
    }

    fun closePreview() {
        _selectedPreviewChannel.value = null
        playerManager.stop()
    }

    fun closePlayer() {
        _isPlayerOpen.value = false
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setFavorite(channel.id, !channel.isFavorite)
        }
    }

    // Only reloads when user presses the Reload button
    fun reloadCurrentPlaylist() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (_selectedPlaylistId.value == 0L) {
                    val (_, totalFetched) = repository.refreshAllSourcesFresh()
                    _toastMessage.value = "Semua 3 sumber M3U berhasil diperbarui ($totalFetched saluran terbaru tersimpan)"
                } else {
                    val playlist = playlists.value.find { it.id == _selectedPlaylistId.value }
                        ?: com.example.data.repository.SampleData.DEFAULT_SOURCES.find { it.id == _selectedPlaylistId.value }
                    if (playlist != null) {
                        val (_, total) = repository.fetchAndSaveOnlinePlaylist(playlist.id, playlist.sourceUrl, playlist.title)
                        _toastMessage.value = "${playlist.title} diperbarui ($total saluran terbaru)"
                    }
                }
            } catch (e: Exception) {
                _toastMessage.value = "Gagal memuat ulang: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addNewOnlinePlaylist(title: String, url: String) {
        if (url.isBlank()) {
            _toastMessage.value = "URL playlist tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val effectiveTitle = title.ifBlank { "Playlist Online" }
                val newId = repository.addNewOnlinePlaylist(effectiveTitle, url.trim())
                _selectedPlaylistId.value = newId
                _toastMessage.value = "Playlist online berhasil ditambahkan dan disimpan ke cache"
            } catch (e: Exception) {
                _toastMessage.value = "Gagal: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun createCustomPlaylist(title: String) {
        if (title.isBlank()) {
            _toastMessage.value = "Nama playlist kustom tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            try {
                val newId = repository.createCustomPlaylist(title.trim())
                _selectedPlaylistId.value = newId
                _toastMessage.value = "Playlist kustom '$title' dibuat"
            } catch (e: Exception) {
                _toastMessage.value = "Gagal membuat playlist: ${e.message}"
            }
        }
    }

    fun updatePlaylistSource(playlistId: Long, newTitle: String, newUrl: String) {
        if (newUrl.isBlank()) {
            _toastMessage.value = "URL baru tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            try {
                repository.updatePlaylistSource(playlistId, newTitle.ifBlank { "Playlist" }, newUrl.trim())
                _playlistToEdit.value = null
                _toastMessage.value = "Sumber link playlist berhasil diganti"
                // Reload from new source
                reloadCurrentPlaylist()
            } catch (e: Exception) {
                _toastMessage.value = "Gagal mengganti source: ${e.message}"
            }
        }
    }

    fun deleteChannel(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChannel(channel.id, channel.playlistId)
            _toastMessage.value = "Video '${channel.name}' dihapus dari playlist"
        }
    }

    fun addChannelToPlaylist(targetPlaylistId: Long, channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addChannelToPlaylist(targetPlaylistId, channel)
            _toastMessage.value = "'${channel.name}' berhasil ditambahkan ke playlist"
        }
    }

    fun importLocalM3u(title: String, uri: Uri) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val effectiveTitle = title.ifBlank { "Playlist Berkas M3U" }
                val newId = repository.importLocalM3uFile(effectiveTitle, uri)
                _selectedPlaylistId.value = newId
                _toastMessage.value = "Berkas M3U berhasil diimpor ke cache lokal"
            } catch (e: Exception) {
                _toastMessage.value = "Gagal mengimpor berkas: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addDirectStream(
        name: String,
        streamUrl: String,
        group: String,
        licenseKey: String? = null,
        licenseType: String? = null
    ) {
        if (streamUrl.isBlank()) {
            _toastMessage.value = "URL streaming tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            try {
                val isMpd = streamUrl.contains(".mpd", ignoreCase = true) ||
                        !licenseKey.isNullOrBlank() ||
                        group.contains("mpd", ignoreCase = true)
                val effectiveName = name.ifBlank {
                    if (isMpd) "Aliran DASH MPD" else "Streaming Langsung"
                }
                val is4k = effectiveName.contains("4K", ignoreCase = true) || streamUrl.contains("4k", ignoreCase = true)
                val currentPId = _selectedPlaylistId.value
                val newChannel = Channel(
                    id = "${currentPId}_manual_${System.currentTimeMillis()}",
                    playlistId = currentPId,
                    name = effectiveName,
                    streamUrl = streamUrl.trim(),
                    groupTitle = group.ifBlank { if (isMpd) "DASH MPD" else "Custom" },
                    licenseKey = licenseKey?.takeIf { it.isNotBlank() },
                    licenseType = licenseType ?: if (!licenseKey.isNullOrBlank()) "clearkey" else null,
                    resolutionLabel = if (is4k) "4K UHD" else if (isMpd) "DASH MPD" else "HD/Auto",
                    is4k = is4k,
                    addedAt = System.currentTimeMillis()
                )
                repository.addDirectChannel(newChannel)
                _toastMessage.value = "Aliran '$effectiveName' siap diputar"
                // Immediately preview
                selectChannelForPreview(newChannel)
            } catch (e: Exception) {
                _toastMessage.value = "Gagal menambahkan: ${e.message}"
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        if (playlistId == 1L) {
            _toastMessage.value = "Playlist sumber utama bawaan tidak dapat dihapus"
            return
        }
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _selectedPlaylistId.value = 1L
            _toastMessage.value = "Playlist dihapus"
        }
    }

    fun setPlaylistToEdit(playlist: Playlist?) { _playlistToEdit.value = playlist }
    fun dismissNotification(notifId: String) {
        _recentNotifications.value = _recentNotifications.value.filter { it.id != notifId }
    }
    fun clearToast() { _toastMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
