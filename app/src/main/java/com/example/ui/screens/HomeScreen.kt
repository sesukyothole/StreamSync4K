package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.data.model.Channel
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val filteredChannels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val allChannels by viewModel.currentChannels.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val selectedPreviewChannel by viewModel.selectedPreviewChannel.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val isLoading by viewModel.playerManager.isLoading.collectAsStateWithLifecycle()
    val isMuted by viewModel.playerManager.isMuted.collectAsStateWithLifecycle()
    val errorMessage by viewModel.playerManager.errorMessage.collectAsStateWithLifecycle()

    val showChannelPopup by viewModel.showChannelPopup.collectAsStateWithLifecycle()
    val deviceSpecSummary by viewModel.deviceSpecSummary.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val activePlaylist = playlists.firstOrNull()

    val playlistToEdit by viewModel.playlistToEdit.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Local M3U file import launcher
    val localM3uPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importLocalM3u("Playlist Berkas M3U", it)
        }
    }

    // Spin animation for reload icon
    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteReload")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    // Notification permission request for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Tapping 1x anywhere on screen automatically shows channel selection popup
                        viewModel.setShowChannelPopup(true)
                    }
                )
            },
        containerColor = ObsidianBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Top Section (Title, playlist source chips, control buttons) -> ~15% height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.15f)
            ) {
                TopHeaderBarWithSourceSelector(
                    channelCount = filteredChannels.size,
                    selectedPlaylistId = selectedPlaylistId,
                    playlists = playlists,
                    isRefreshing = isRefreshing,
                    rotationAngle = rotationAngle,
                    onSelectPlaylist = { viewModel.selectPlaylist(it) },
                    onReload = { viewModel.reloadCurrentPlaylist() },
                    onEditSource = { activePlaylist?.let { viewModel.setPlaylistToEdit(it) } },
                    onCloseApp = { (context as? Activity)?.finishAffinity() }
                )
            }

            // Live Streaming Player Section -> ~85% height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                if (selectedPreviewChannel != null) {
                    LivePreviewPlayerSection(
                        channel = selectedPreviewChannel!!,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        isMuted = isMuted,
                        errorMessage = errorMessage,
                        playerManager = viewModel.playerManager,
                        onTogglePlay = { viewModel.playerManager.togglePlayPause() },
                        onToggleMute = { viewModel.playerManager.toggleMute() },
                        onFullScreen = { viewModel.openFullScreen() },
                        onOpenSettings = { viewModel.openFullScreen() }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Filled.LiveTv, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ketuk 1x di layar untuk memilih saluran siaran",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = deviceSpecSummary,
                                color = NeonCyan,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Channel Selection Popup (shown on launch and on 1x tap)
    if (showChannelPopup) {
        ChannelSelectionPopup(
            channels = allChannels.ifEmpty { filteredChannels },
            deviceSpecSummary = deviceSpecSummary,
            onSelectChannel = { channel ->
                viewModel.selectChannelForPreview(channel)
                viewModel.setShowChannelPopup(false)
            },
            onDismiss = {
                viewModel.setShowChannelPopup(false)
            }
        )
    }

    // Edit Source Link Dialog
    playlistToEdit?.let { playlist ->
        EditPlaylistSourceDialog(
            playlist = playlist,
            allPlaylists = playlists,
            onSaveNewSource = { playlistId, newTitle, newUrl ->
                viewModel.updatePlaylistSource(playlistId, newTitle, newUrl)
            },
            onAddNewOnlineSource = { title, url ->
                viewModel.addNewOnlinePlaylist(title, url)
            },
            onImportLocalM3u = {
                localM3uPickerLauncher.launch("*/*")
            },
            onDeleteSource = { id ->
                viewModel.deletePlaylist(id)
            },
            onDismiss = { viewModel.setPlaylistToEdit(null) }
        )
    }
}

@Composable
private fun TopHeaderBarWithSourceSelector(
    channelCount: Int,
    selectedPlaylistId: Long,
    playlists: List<com.example.data.model.Playlist>,
    isRefreshing: Boolean,
    rotationAngle: Float,
    onSelectPlaylist: (Long) -> Unit,
    onReload: () -> Unit,
    onEditSource: () -> Unit,
    onCloseApp: () -> Unit
) {
    Surface(
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title & Count
            Column(modifier = Modifier.widthIn(max = 130.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(LiveRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "StreamSync",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = TextWhite
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (channelCount > 0) "$channelCount Saluran" else "Memuat...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Playlist Source Selector integrated in Control Bar
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val isAll = selectedPlaylistId == 0L
                    FilterChip(
                        selected = isAll,
                        onClick = { onSelectPlaylist(0L) },
                        label = {
                            Text(
                                "Semua",
                                fontSize = 10.sp,
                                fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextMuted,
                            selectedContainerColor = ElectricBlue,
                            selectedLabelColor = Color.Black
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAll,
                            borderColor = if (isAll) ElectricBlue else Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                items(com.example.data.repository.SampleData.DEFAULT_SOURCES) { source ->
                    val isSelected = selectedPlaylistId == source.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPlaylist(source.id) },
                        label = {
                            Text(
                                source.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextMuted,
                            selectedContainerColor = ElectricBlue,
                            selectedLabelColor = Color.Black
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) ElectricBlue else Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEditSource,
                    modifier = Modifier.size(32.dp).testTag("action_edit_source_button")
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Ganti Sumber",
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onReload,
                    enabled = !isRefreshing,
                    modifier = Modifier.size(32.dp).testTag("action_reload_button")
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Muat Ulang",
                        tint = if (isRefreshing) NeonCyan else TextWhite,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (isRefreshing) rotationAngle else 0f)
                    )
                }

                IconButton(
                    onClick = onCloseApp,
                    modifier = Modifier.size(32.dp).testTag("action_close_app_button")
                ) {
                    Icon(
                        Icons.Filled.PowerSettingsNew,
                        contentDescription = "Tutup",
                        tint = LiveRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LivePreviewPlayerSection(
    channel: Channel,
    isPlaying: Boolean,
    isLoading: Boolean,
    isMuted: Boolean,
    errorMessage: String?,
    playerManager: com.example.player.PlayerManager,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onFullScreen: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Video Player Box (Occupies upper part of 85% area)
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .testTag("live_preview_player_section")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ExoPlayer AndroidView
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = playerManager.exoPlayer
                            useController = false
                            keepScreenOn = true
                        }
                    },
                    update = { playerView ->
                        playerView.player = playerManager.exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top Banner with Live Badge and Channel Name (NO CLOSE BUTTON)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(LiveRed, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "● LIVE",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = channel.name,
                            color = TextWhite,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Loading Indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Overlay Bar (Controls + Double Click Hint)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Play / Pause
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Mute / Unmute
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Nyalakan Suara" else "Bisukan",
                                tint = TextWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Full Screen / Popup Hint
                    Box(
                        modifier = Modifier
                            .background(NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { onFullScreen() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Fullscreen,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Layar Penuh",
                                color = NeonCyan,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Error banner if any
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = LiveRed,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Tanda Pengaturan Alur Video di bawah live streaming
        Surface(
            color = DarkSurfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clickable { onOpenSettings() }
                .border(0.5.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Pengaturan Alur Video",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pengaturan Alur Video (${channel.resolutionLabel})",
                        color = TextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Ketuk Atur ⚙",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
