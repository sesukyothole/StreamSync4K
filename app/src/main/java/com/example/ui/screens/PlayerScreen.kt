package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.AspectRatioMode
import com.example.player.PlayerManager
import com.example.ui.components.PlayerControlsOverlay
import com.example.ui.components.QualityResolutionSheet
import com.example.ui.components.SubtitleOverlayView
import com.example.ui.components.SubtitleSyncSheet
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    playerManager: PlayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val isLoading by playerManager.isLoading.collectAsStateWithLifecycle()
    val isLive by playerManager.isLive.collectAsStateWithLifecycle()
    val currentPosition by playerManager.currentPosition.collectAsStateWithLifecycle()
    val duration by playerManager.duration.collectAsStateWithLifecycle()
    val videoSize by playerManager.videoSize.collectAsStateWithLifecycle()
    val bitrate by playerManager.videoBitrate.collectAsStateWithLifecycle()
    val aspectRatioMode by playerManager.aspectRatioMode.collectAsStateWithLifecycle()
    val playbackSpeed by playerManager.playbackSpeed.collectAsStateWithLifecycle()
    val currentChannel by playerManager.currentChannel.collectAsStateWithLifecycle()
    val errorMessage by playerManager.errorMessage.collectAsStateWithLifecycle()

    val subtitleSettings by playerManager.subtitleSettings.collectAsStateWithLifecycle()
    val activeSubtitleText by playerManager.activeSubtitleText.collectAsStateWithLifecycle()
    val subtitleTracks by playerManager.subtitleTracks.collectAsStateWithLifecycle()
    val videoTracks by playerManager.videoTracks.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(true) }
    var showSubtitleSyncSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }

    // Subtitle File Picker (.srt or .vtt)
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            playerManager.loadSubtitleFromUri(it, "File Lokal")
        }
    }

    // Auto-hide controls after 4.5 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4500)
            showControls = false
        }
    }

    // Back button handling
    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_container")
    ) {
        // ExoPlayer AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.exoPlayer
                    useController = false
                    keepScreenOn = true
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    (videoSurfaceView as? android.view.SurfaceView)?.setZOrderMediaOverlay(true)
                }
            },
            update = { playerView ->
                playerView.player = playerManager.exoPlayer
                playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                (playerView.videoSurfaceView as? android.view.SurfaceView)?.setZOrderMediaOverlay(true)
                playerView.resizeMode = when (aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Real-time Subtitle Overlay
        SubtitleOverlayView(
            text = activeSubtitleText,
            settings = subtitleSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Buffering Indicator
        AnimatedVisibility(
            visible = isLoading && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = NeonCyan,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Menghubungkan aliran streaming...",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }

        // Error Banner
        errorMessage?.let { error ->
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceVariant.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = LiveRed,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gagal Memutar Siaran",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextWhite)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { currentChannel?.let { playerManager.playChannel(it) } },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Coba Putar Ulang")
                    }
                }
            }
        }

        // On-Screen Controls Overlay
        PlayerControlsOverlay(
            isVisible = showControls,
            channel = currentChannel,
            isPlaying = isPlaying,
            isLive = isLive,
            currentPositionMs = currentPosition,
            durationMs = duration,
            aspectRatioMode = aspectRatioMode,
            subtitleOffsetMs = subtitleSettings.offsetMs,
            subtitlesEnabled = subtitleSettings.isEnabled,
            onToggleControls = { showControls = !showControls },
            onBack = onBack,
            onPlayPause = { playerManager.togglePlayPause() },
            onSeekRelative = { playerManager.seekRelative(it) },
            onSeekToFraction = { playerManager.seekToFraction(it) },
            onCycleAspectRatio = { playerManager.cycleAspectRatio() },
            onOpenSubtitleSync = { showSubtitleSyncSheet = true },
            onOpenQualitySettings = { showQualitySheet = true },
            onEnterPiP = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val activity = context as? Activity
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                    activity?.enterPictureInPictureMode(params)
                }
            }
        )

        // Subtitle Sync Bottom Sheet
        if (showSubtitleSyncSheet) {
            SubtitleSyncSheet(
                settings = subtitleSettings,
                tracks = subtitleTracks,
                onToggleEnabled = { playerManager.setSubtitlesEnabled(it) },
                onOffsetChange = { playerManager.setSubtitleOffset(it) },
                onOffsetAdjust = { playerManager.adjustSubtitleOffset(it) },
                onFontSizeChange = { playerManager.setSubtitleFontSize(it) },
                onTextColorChange = { playerManager.setSubtitleTextColor(it) },
                onBackgroundBoxToggle = { playerManager.setSubtitleBackgroundBox(it) },
                onPickLocalSubtitle = { subtitlePickerLauncher.launch("*/*") },
                onLoadOnlineSubtitle = { url -> playerManager.loadSubtitleFromUrl(url, "Subtitle Online") },
                onDismiss = { showSubtitleSyncSheet = false }
            )
        }

        // Quality & Resolution Bottom Sheet
        if (showQualitySheet) {
            QualityResolutionSheet(
                videoSize = videoSize,
                bitrate = bitrate,
                videoTracks = videoTracks,
                aspectRatioMode = aspectRatioMode,
                playbackSpeed = playbackSpeed,
                onSelectResolution = { playerManager.setPreferredResolution(it) },
                onSelectAspectRatio = { playerManager.setAspectRatioMode(it) },
                onSelectSpeed = { playerManager.setPlaybackSpeed(it) },
                onDismiss = { showQualitySheet = false }
            )
        }
    }
}
