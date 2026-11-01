package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.data.model.AspectRatioMode
import com.example.data.model.Channel
import com.example.data.model.SubtitleCue
import com.example.data.model.SubtitleSettings
import com.example.data.model.SubtitleTrackInfo
import com.example.data.model.VideoTrackInfo
import com.example.data.parser.SrtVttParser
import com.example.data.repository.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(UnstableApi::class)
class PlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    // Multi-screen player slots (up to 8 screens)
    private val slotPlayers = Array<ExoPlayer?>(8) { null }

    val exoPlayer: ExoPlayer
        get() = getPlayer(0)

    private val _slotChannels = MutableStateFlow<List<Channel?>>(listOf(null, null, null, null, null, null, null, null))
    val slotChannels: StateFlow<List<Channel?>> = _slotChannels.asStateFlow()

    private val _multiScreenCount = MutableStateFlow(1) // 1, 2, 4, 6, 8
    val multiScreenCount: StateFlow<Int> = _multiScreenCount.asStateFlow()

    private val _activeSlotIndex = MutableStateFlow(0)
    val activeSlotIndex: StateFlow<Int> = _activeSlotIndex.asStateFlow()

    // State of active player slot
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _videoSize = MutableStateFlow(VideoSize.UNKNOWN)
    val videoSize: StateFlow<VideoSize> = _videoSize.asStateFlow()

    private val _videoBitrate = MutableStateFlow(0)
    val videoBitrate: StateFlow<Int> = _videoBitrate.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Subtitles
    private val _subtitleSettings = MutableStateFlow(SubtitleSettings())
    val subtitleSettings: StateFlow<SubtitleSettings> = _subtitleSettings.asStateFlow()

    private val _loadedSubtitleCues = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val loadedSubtitleCues: StateFlow<List<SubtitleCue>> = _loadedSubtitleCues.asStateFlow()

    private val _activeSubtitleText = MutableStateFlow<String?>(null)
    val activeSubtitleText: StateFlow<String?> = _activeSubtitleText.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<SubtitleTrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<SubtitleTrackInfo>> = _subtitleTracks.asStateFlow()

    private val _videoTracks = MutableStateFlow<List<VideoTrackInfo>>(emptyList())
    val videoTracks: StateFlow<List<VideoTrackInfo>> = _videoTracks.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var progressJob: Job? = null

    init {
        // Initialize primary player for slot 0
        getPlayer(0)
        startProgressTracking()
    }

    fun getPlayer(slotIndex: Int): ExoPlayer {
        val clamped = slotIndex.coerceIn(0, 5)
        var player = slotPlayers[clamped]
        if (player == null) {
            player = createPlayerInstance(clamped == 0)
            slotPlayers[clamped] = player
            setupSlotListener(player, clamped)
        }
        return player
    }

    private fun createPlayerInstance(isPrimary: Boolean): ExoPlayer {
        // Enable decoder fallback so MPD streams with software/hardware codecs decode reliably without black screen
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_500,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_200,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val selector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setPreferredVideoMimeTypes(
                    androidx.media3.common.MimeTypes.VIDEO_H264,
                    androidx.media3.common.MimeTypes.VIDEO_H265,
                    androidx.media3.common.MimeTypes.VIDEO_VP9
                )
                .build()
        }

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(selector)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .build()

        // Only active slot produces sound; secondary slots muted to prevent audio chaos
        player.volume = if (isPrimary && !_isMuted.value) 1f else 0f
        return player
    }

    private fun setupSlotListener(player: ExoPlayer, slotIndex: Int) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (slotIndex == _activeSlotIndex.value) {
                    _isPlaying.value = isPlaying
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (slotIndex == _activeSlotIndex.value) {
                    _isLoading.value = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = player.duration.coerceAtLeast(0L)
                        _isLive.value = player.isCurrentMediaItemLive
                        _errorMessage.value = null
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (slotIndex == _activeSlotIndex.value) {
                    _videoSize.value = videoSize
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                if (slotIndex == _activeSlotIndex.value) {
                    updateAvailableTracks(tracks)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (slotIndex == _activeSlotIndex.value) {
                    _errorMessage.value = "Format streaming MPD/HLS gagal diputar (${error.errorCodeName})."
                    _isLoading.value = false
                }
            }
        })
    }

    fun setActiveSlot(slotIndex: Int) {
        val clamped = slotIndex.coerceIn(0, 5)
        _activeSlotIndex.value = clamped

        val channel = _slotChannels.value.getOrNull(clamped)
        if (channel != null) {
            _currentChannel.value = channel
        }

        val player = getPlayer(clamped)
        _isPlaying.value = player.isPlaying
        _isLoading.value = player.playbackState == Player.STATE_BUFFERING
        _duration.value = player.duration.coerceAtLeast(0L)
        _currentPosition.value = player.currentPosition
        _isLive.value = player.isCurrentMediaItemLive

        // Unmute active slot; mute all others
        for (i in 0 until 8) {
            slotPlayers[i]?.let { p ->
                p.volume = if (i == clamped && !_isMuted.value) 1f else 0f
            }
        }
    }

    fun setMultiScreenCount(count: Int) {
        val valid = when (count) {
            2 -> 2
            4 -> 4
            6 -> 6
            8 -> 8
            else -> 1
        }
        _multiScreenCount.value = valid

        // Stop slots beyond current screen count to preserve memory
        for (i in valid until 8) {
            slotPlayers[i]?.stop()
        }

        val updated = _slotChannels.value.toMutableList()
        for (i in valid until 8) {
            updated[i] = null
        }
        _slotChannels.value = updated

        if (_activeSlotIndex.value >= valid) {
            setActiveSlot(0)
        }

        // Initialize players for all visible slots
        for (i in 0 until valid) {
            getPlayer(i)
        }
    }

    fun playChannel(channel: Channel) {
        playChannelInSlot(_activeSlotIndex.value, channel)
    }

    fun playChannelInSlot(slotIndex: Int, channel: Channel) {
        val clamped = slotIndex.coerceIn(0, 7)
        val updated = _slotChannels.value.toMutableList()
        updated[clamped] = channel
        _slotChannels.value = updated

        if (clamped == _activeSlotIndex.value) {
            _currentChannel.value = channel
            _errorMessage.value = null
            _isLoading.value = true
        }

        val player = getPlayer(clamped)
        val mediaSource = buildMediaSource(channel)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true

        // Maintain audio isolation
        player.volume = if (clamped == _activeSlotIndex.value && !_isMuted.value) 1f else 0f

        // Preload subtitle if 4K
        if (channel.is4k && clamped == _activeSlotIndex.value) {
            loadSubtitleFromText(SampleData.SAMPLE_SRT_SUBTITLE, "Indonesia (Demo 4K)")
        }
    }

    fun buildMediaSource(channel: Channel): MediaSource {
        val cleanUrl = channel.streamUrl.trim()
        val isMpd = cleanUrl.contains(".mpd", ignoreCase = true) ||
                cleanUrl.contains("/mpd", ignoreCase = true) ||
                cleanUrl.contains("manifest_type=mpd", ignoreCase = true) ||
                cleanUrl.contains("dash", ignoreCase = true) ||
                cleanUrl.contains(".mpd?", ignoreCase = true) ||
                channel.licenseType?.contains("mpd", ignoreCase = true) == true ||
                channel.licenseType?.contains("clearkey", ignoreCase = true) == true ||
                channel.name.contains("mpd", ignoreCase = true)

        // Browser User-Agent prevents CDN edge blocking on DASH MPD / IPTV streams
        val defaultUa = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        val userAgent = channel.userAgent?.takeIf { it.isNotBlank() } ?: defaultUa

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to userAgent,
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(Uri.parse(cleanUrl))

        if (cleanUrl.contains(".m3u8", ignoreCase = true)) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
        } else if (isMpd) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
        }

        // Configure ClearKey DRM for MPD / DASH streams
        if (!channel.licenseKey.isNullOrBlank()) {
            val key = channel.licenseKey.trim()
            if (key.startsWith("http://", ignoreCase = true) || key.startsWith("https://", ignoreCase = true)) {
                mediaItemBuilder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                        .setLicenseUri(Uri.parse(key))
                        .build()
                )
            } else {
                try {
                    val drmCallback = createClearKeyCallback(key)
                    val drmSessionManager = androidx.media3.exoplayer.drm.DefaultDrmSessionManager.Builder()
                        .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER)
                        .setMultiSession(false)
                        .build(drmCallback)

                    mediaSourceFactory.setDrmSessionManagerProvider { drmSessionManager }
                    mediaItemBuilder.setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                            .setMultiSession(false)
                            .build()
                    )
                } catch (_: Exception) {
                    // Fallback to standard provider
                }
            }
        }

        return mediaSourceFactory.createMediaSource(mediaItemBuilder.build())
    }

    private fun createClearKeyCallback(keySpec: String): androidx.media3.exoplayer.drm.MediaDrmCallback {
        val trimmed = keySpec.trim()
        if (trimmed.startsWith("{") && trimmed.contains("\"keys\"")) {
            return androidx.media3.exoplayer.drm.LocalMediaDrmCallback(trimmed.toByteArray(Charsets.UTF_8))
        }

        val keyPairs = trimmed.split(',', ';', '\n').filter { it.contains(":") }
        val keysJsonList = mutableListOf<String>()
        for (pair in keyPairs) {
            val parts = pair.split(":")
            if (parts.size >= 2) {
                val kidB64 = normalizeKeyToBase64Url(parts[0])
                val keyB64 = normalizeKeyToBase64Url(parts[1])
                keysJsonList.add("""{"kty":"oct","k":"$keyB64","kid":"$kidB64"}""")
            }
        }

        val jsonString = if (keysJsonList.isNotEmpty()) {
            """{"keys":[${keysJsonList.joinToString(",")}], "type":"temporary"}"""
        } else {
            trimmed
        }
        return androidx.media3.exoplayer.drm.LocalMediaDrmCallback(jsonString.toByteArray(Charsets.UTF_8))
    }

    private fun normalizeKeyToBase64Url(raw: String): String {
        val clean = raw.trim().replace("-", "").replace(" ", "")
        val isHex = clean.length == 32 && clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        return if (isHex) {
            val bytes = ByteArray(clean.length / 2)
            for (i in bytes.indices) {
                val index = i * 2
                bytes[i] = clean.substring(index, index + 2).toInt(16).toByte()
            }
            android.util.Base64.encodeToString(
                bytes,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            )
        } else {
            raw.trim().replace("=", "").replace("+", "-").replace("/", "_")
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        val activePlayer = getPlayer(_activeSlotIndex.value)
        activePlayer.volume = if (newMute) 0f else 1f
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        val activePlayer = getPlayer(_activeSlotIndex.value)
        activePlayer.volume = if (muted) 0f else 1f
    }

    fun setVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        _isMuted.value = (clamped == 0f)
        val activePlayer = getPlayer(_activeSlotIndex.value)
        activePlayer.volume = clamped
    }

    fun pause() {
        getPlayer(_activeSlotIndex.value).pause()
    }

    fun stop() {
        for (i in 0 until 8) {
            slotPlayers[i]?.stop()
        }
    }

    fun togglePlayPause() {
        val player = getPlayer(_activeSlotIndex.value)
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekRelative(offsetMs: Long) {
        val player = getPlayer(_activeSlotIndex.value)
        val target = (player.currentPosition + offsetMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
    }

    fun seekToFraction(fraction: Float) {
        val player = getPlayer(_activeSlotIndex.value)
        val dur = player.duration
        if (dur > 0) {
            player.seekTo((dur * fraction).toLong())
        }
    }

    fun setAspectRatioMode(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun cycleAspectRatio() {
        val modes = AspectRatioMode.entries
        val nextIndex = (modes.indexOf(_aspectRatioMode.value) + 1) % modes.size
        _aspectRatioMode.value = modes[nextIndex]
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        getPlayer(_activeSlotIndex.value).playbackParameters = PlaybackParameters(speed)
    }

    // --- Subtitle Synchronization ---

    fun setSubtitleOffset(offsetMs: Long) {
        _subtitleSettings.value = _subtitleSettings.value.copy(offsetMs = offsetMs)
        updateActiveSubtitleCue()
    }

    fun adjustSubtitleOffset(deltaMs: Long) {
        val newOffset = _subtitleSettings.value.offsetMs + deltaMs
        setSubtitleOffset(newOffset)
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        _subtitleSettings.value = _subtitleSettings.value.copy(isEnabled = enabled)
        if (!enabled) {
            _activeSubtitleText.value = null
        } else {
            updateActiveSubtitleCue()
        }
    }

    fun setSubtitleFontSize(sizeSp: Float) {
        _subtitleSettings.value = _subtitleSettings.value.copy(fontSizeSp = sizeSp)
    }

    fun setSubtitleTextColor(colorHex: Long) {
        _subtitleSettings.value = _subtitleSettings.value.copy(textColorHex = colorHex)
    }

    fun setSubtitleBackgroundBox(enabled: Boolean) {
        _subtitleSettings.value = _subtitleSettings.value.copy(hasBackgroundBox = enabled)
    }

    fun loadSubtitleFromUri(uri: Uri, label: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: return@launch
                val cues = SrtVttParser.parse(content)
                _loadedSubtitleCues.value = cues
                val currentTracks = _subtitleTracks.value.toMutableList()
                currentTracks.add(SubtitleTrackInfo("ext_${System.currentTimeMillis()}", "id", label, isExternal = true, uri = uri.toString()))
                _subtitleTracks.value = currentTracks
                _subtitleSettings.value = _subtitleSettings.value.copy(isEnabled = true)
                updateActiveSubtitleCue()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat subtitle dari berkas: ${e.message}"
            }
        }
    }

    fun loadSubtitleFromUrl(url: String, label: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@launch
                val cues = SrtVttParser.parse(body)
                _loadedSubtitleCues.value = cues
                val currentTracks = _subtitleTracks.value.toMutableList()
                currentTracks.add(SubtitleTrackInfo("url_${System.currentTimeMillis()}", "und", label, isExternal = true, uri = url))
                _subtitleTracks.value = currentTracks
                _subtitleSettings.value = _subtitleSettings.value.copy(isEnabled = true)
                updateActiveSubtitleCue()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengunduh subtitle online: ${e.message}"
            }
        }
    }

    fun loadSubtitleFromText(content: String, label: String) {
        val cues = SrtVttParser.parse(content)
        _loadedSubtitleCues.value = cues
        val currentTracks = _subtitleTracks.value.toMutableList()
        currentTracks.add(SubtitleTrackInfo("demo_${System.currentTimeMillis()}", "id", label, isExternal = true))
        _subtitleTracks.value = currentTracks
        _subtitleSettings.value = _subtitleSettings.value.copy(isEnabled = true)
        updateActiveSubtitleCue()
    }

    // --- Quality & Resolution Selection ---

    fun setPreferredResolution(maxHeight: Int) {
        val activePlayer = getPlayer(_activeSlotIndex.value)
        val trackSelector = activePlayer.trackSelector as? DefaultTrackSelector ?: return
        val builder = trackSelector.buildUponParameters()
        if (maxHeight <= 0) {
            builder.clearVideoSizeConstraints()
                .setMaxVideoSize(3840, 2160)
        } else {
            val maxWidth = (maxHeight * 16) / 9
            builder.setMaxVideoSize(maxWidth, maxHeight)
                .setMinVideoSize(maxWidth / 2, maxHeight / 2)
        }
        trackSelector.setParameters(builder)
    }

    private fun updateAvailableTracks(tracks: Tracks) {
        val vTracks = mutableListOf<VideoTrackInfo>()
        val sTracks = mutableListOf<SubtitleTrackInfo>()

        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val isSelected = trackGroup.isTrackSelected(i)
                    val label = when {
                        format.height >= 2160 -> "4K UHD (${format.width}x${format.height})"
                        format.height >= 1080 -> "1080p FHD (${format.width}x${format.height})"
                        format.height >= 720 -> "720p HD (${format.width}x${format.height})"
                        format.height >= 480 -> "480p SD (${format.width}x${format.height})"
                        format.height > 0 -> "${format.height}p"
                        else -> "Video Track ${i + 1}"
                    }
                    vTracks.add(
                        VideoTrackInfo(
                            id = format.id ?: "$i",
                            width = format.width,
                            height = format.height,
                            bitrate = format.bitrate,
                            label = label,
                            isSelected = isSelected
                        )
                    )
                }
            } else if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val isSelected = trackGroup.isTrackSelected(i)
                    val label = format.label ?: format.language ?: "Subtitle ${i + 1}"
                    sTracks.add(
                        SubtitleTrackInfo(
                            id = format.id ?: "$i",
                            language = format.language ?: "und",
                            label = label,
                            isExternal = false
                        )
                    )
                }
            }
        }
        _videoTracks.value = vTracks
        _subtitleTracks.value = sTracks
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val activePlayer = getPlayer(_activeSlotIndex.value)
                if (activePlayer.isPlaying) {
                    _currentPosition.value = activePlayer.currentPosition
                    _duration.value = activePlayer.duration.coerceAtLeast(0L)
                    updateActiveSubtitleCue()
                }
                delay(250)
            }
        }
    }

    private fun updateActiveSubtitleCue() {
        if (!_subtitleSettings.value.isEnabled) {
            _activeSubtitleText.value = null
            return
        }

        val cues = _loadedSubtitleCues.value
        if (cues.isEmpty()) {
            _activeSubtitleText.value = null
            return
        }

        val effectivePosition = getPlayer(_activeSlotIndex.value).currentPosition + _subtitleSettings.value.offsetMs
        val matchingCue = cues.firstOrNull { cue ->
            effectivePosition in cue.startTimeMs..cue.endTimeMs
        }

        _activeSubtitleText.value = matchingCue?.text
    }

    fun release() {
        progressJob?.cancel()
        for (i in 0 until 8) {
            slotPlayers[i]?.release()
            slotPlayers[i] = null
        }
    }
}
