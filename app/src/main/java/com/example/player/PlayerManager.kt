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
    private val trackSelector = DefaultTrackSelector(context)
    val exoPlayer: ExoPlayer

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
        // High quality rendering with extension support
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // Fast low-latency buffer configuration for instant live stream playback
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_500,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_200,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .build()

        setupPlayerListener()
        startProgressTracking()
    }

    private var errorRetryCount = 0

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    errorRetryCount = 0
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isLoading.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                    _isLive.value = exoPlayer.isCurrentMediaItemLive
                    _errorMessage.value = null
                    errorRetryCount = 0
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _videoSize.value = videoSize
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateAvailableTracks(tracks)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val current = _currentChannel.value
                if (current != null && errorRetryCount < SampleData.STABLE_FALLBACK_STREAMS.size) {
                    val fallbackUrl = SampleData.STABLE_FALLBACK_STREAMS[errorRetryCount]
                    errorRetryCount++
                    val altChannel = current.copy(streamUrl = fallbackUrl)
                    playChannel(altChannel)
                } else {
                    _errorMessage.value = "Format streaming gagal diputar (${error.errorCodeName}). Silakan coba saluran lain."
                    _isLoading.value = false
                }
            }
        })
    }

    fun playChannel(channel: Channel) {
        _currentChannel.value = channel
        _errorMessage.value = null
        _isLoading.value = true

        val userAgent = channel.userAgent ?: "StreamSync/1.0 (Android; ExoPlayer)"
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val cleanUrl = channel.streamUrl.trim()
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(Uri.parse(cleanUrl))

        val isMpd = cleanUrl.contains(".mpd", ignoreCase = true) ||
                cleanUrl.contains("/mpd", ignoreCase = true) ||
                cleanUrl.contains("manifest_type=mpd", ignoreCase = true) ||
                cleanUrl.contains("dash", ignoreCase = true) ||
                channel.licenseType?.contains("mpd", ignoreCase = true) == true ||
                channel.licenseType?.contains("clearkey", ignoreCase = true) == true ||
                channel.name.contains("mpd", ignoreCase = true)

        if (cleanUrl.contains(".m3u8", ignoreCase = true)) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
        } else if (isMpd) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
        }

        // Configure ClearKey DRM for MPD / encrypted streams if provided
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
                } catch (_: Exception) {
                    // Fallback to standard provider
                }
            }
        }

        // Pre-load default Indonesian/English demo subtitle for 4K test streams or user test
        if (channel.is4k) {
            loadSubtitleFromText(SampleData.SAMPLE_SRT_SUBTITLE, "Indonesia (Demo 4K)")
        }

        val mediaSource = mediaSourceFactory.createMediaSource(mediaItemBuilder.build())
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun createClearKeyCallback(keySpec: String): androidx.media3.exoplayer.drm.MediaDrmCallback {
        val jsonString = if (keySpec.contains(":")) {
            val parts = keySpec.split(":")
            val kidHex = parts[0].trim()
            val keyHex = parts[1].trim()
            val kidB64 = hexToBase64Url(kidHex)
            val keyB64 = hexToBase64Url(keyHex)
            """{"keys":[{"kty":"oct","k":"$keyB64","kid":"$kidB64"}],"type":"temporary"}"""
        } else {
            keySpec
        }
        return androidx.media3.exoplayer.drm.LocalMediaDrmCallback(jsonString.toByteArray(Charsets.UTF_8))
    }

    private fun hexToBase64Url(hex: String): String {
        val cleanHex = hex.replace("-", "").replace(" ", "")
        val bytes = ByteArray(cleanHex.length / 2)
        for (i in bytes.indices) {
            val index = i * 2
            bytes[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
        }
        return android.util.Base64.encodeToString(
            bytes,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
        )
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        exoPlayer.volume = if (newMute) 0f else 1f
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        exoPlayer.volume = if (muted) 0f else 1f
    }

    fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    fun stop() {
        exoPlayer.stop()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekRelative(offsetMs: Long) {
        val target = (exoPlayer.currentPosition + offsetMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
    }

    fun seekToFraction(fraction: Float) {
        val dur = exoPlayer.duration
        if (dur > 0) {
            exoPlayer.seekTo((dur * fraction).toLong())
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
        exoPlayer.playbackParameters = PlaybackParameters(speed)
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
        // maxHeight: 2160 for 4K UHD, 1080 for FHD, 720 for HD, 480 for SD, 0 for Auto
        val builder = trackSelector.buildUponParameters()
        if (maxHeight <= 0) {
            // Auto / Adaptive: Allow up to 4K UHD (3840x2160)
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
                    if (isSelected) {
                        _videoBitrate.value = format.bitrate
                    }
                }
            } else if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val lang = format.language ?: "und"
                    val label = format.label ?: "Subtitle ($lang)"
                    sTracks.add(
                        SubtitleTrackInfo(
                            id = "internal_${trackGroup.mediaTrackGroup.id}_$i",
                            language = lang,
                            label = label,
                            isExternal = false
                        )
                    )
                }
            }
        }
        _videoTracks.value = vTracks
        // Merge with existing external tracks
        val existingExternal = _subtitleTracks.value.filter { it.isExternal }
        _subtitleTracks.value = existingExternal + sTracks
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPosition.value = exoPlayer.currentPosition
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                    updateActiveSubtitleCue()
                }
                delay(100)
            }
        }
    }

    private fun updateActiveSubtitleCue() {
        val settings = _subtitleSettings.value
        if (!settings.isEnabled || _loadedSubtitleCues.value.isEmpty()) {
            _activeSubtitleText.value = null
            return
        }

        val cue = SrtVttParser.getActiveCue(
            cues = _loadedSubtitleCues.value,
            currentPositionMs = exoPlayer.currentPosition,
            offsetMs = settings.offsetMs
        )
        _activeSubtitleText.value = cue?.text
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer.release()
    }
}
