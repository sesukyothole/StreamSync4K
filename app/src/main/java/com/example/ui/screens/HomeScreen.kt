package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
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

    val multiScreenCount by viewModel.multiScreenCount.collectAsStateWithLifecycle()
    val activeSlotIndex by viewModel.activeSlotIndex.collectAsStateWithLifecycle()
    val slotChannels by viewModel.slotChannels.collectAsStateWithLifecycle()
    val selectedPreviewChannel by viewModel.selectedPreviewChannel.collectAsStateWithLifecycle()

    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val isLoading by viewModel.playerManager.isLoading.collectAsStateWithLifecycle()
    val isMuted by viewModel.playerManager.isMuted.collectAsStateWithLifecycle()
    val errorMessage by viewModel.playerManager.errorMessage.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.playerManager.currentPosition.collectAsStateWithLifecycle()
    val durationMs by viewModel.playerManager.duration.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playerManager.playbackSpeed.collectAsStateWithLifecycle()
    val isLive by viewModel.playerManager.isLive.collectAsStateWithLifecycle()

    val showChannelPopup by viewModel.showChannelPopup.collectAsStateWithLifecycle()
    val deviceSpecSummary by viewModel.deviceSpecSummary.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val activePlaylist = playlists.firstOrNull()

    val playlistToEdit by viewModel.playlistToEdit.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAudioDialog by remember { mutableStateOf(false) }

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
            // Top Section (Compact Height: Title, Playlist info & Multi-Screen controls below title)
            TopHeaderBarWithSourceSelector(
                channelCount = filteredChannels.size,
                selectedPlaylistId = selectedPlaylistId,
                playlists = playlists,
                multiScreenCount = multiScreenCount,
                isRefreshing = isRefreshing,
                rotationAngle = rotationAngle,
                onSelectPlaylist = { viewModel.selectPlaylist(it) },
                onSelectMultiScreen = { viewModel.setMultiScreenCount(it) },
                onReload = { viewModel.reloadCurrentPlaylist() },
                onEditSource = { activePlaylist?.let { viewModel.setPlaylistToEdit(it) } },
                onCloseApp = { (context as? Activity)?.finishAffinity() }
            )

            // Live Streaming Multi-Screen Section -> Expands to fill ALL remaining screen height!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val hasAnyChannel = slotChannels.any { it != null } || selectedPreviewChannel != null || multiScreenCount > 1
                if (hasAnyChannel) {
                    LivePreviewPlayerSection(
                        multiScreenCount = multiScreenCount,
                        activeSlotIndex = activeSlotIndex,
                        slotChannels = slotChannels,
                        activeChannel = selectedPreviewChannel,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        isMuted = isMuted,
                        errorMessage = errorMessage,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        playbackSpeed = playbackSpeed,
                        isLive = isLive,
                        playerManager = viewModel.playerManager,
                        onSelectSlot = { slot ->
                            viewModel.setActiveSlot(slot)
                        },
                        onOpenChannelPickerForSlot = { slot ->
                            viewModel.setActiveSlot(slot)
                            viewModel.setShowChannelPopup(true)
                        },
                        onPreviousChannel = { slot ->
                            viewModel.previousChannel(slot)
                        },
                        onNextChannel = { slot ->
                            viewModel.nextChannel(slot)
                        },
                        onOpenAudioSettings = {
                            showAudioDialog = true
                        },
                        onTogglePlay = { viewModel.playerManager.togglePlayPause() },
                        onToggleMute = { viewModel.playerManager.toggleMute() },
                        onSeekToFraction = { viewModel.playerManager.seekToFraction(it) },
                        onSeekRelative = { viewModel.playerManager.seekRelative(it) },
                        onSetPlaybackSpeed = { viewModel.playerManager.setPlaybackSpeed(it) },
                        onFullScreen = { channel ->
                            viewModel.playChannelFullScreen(channel)
                        }
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

    // Audio Volume Control Dialog (0 - 100 Scroll Slider with Device Hardware Sync)
    if (showAudioDialog) {
        AudioVolumeDialog(
            playerManager = viewModel.playerManager,
            onDismiss = { showAudioDialog = false }
        )
    }

    // Channel Selection Popup (shown on launch, on 1x tap, or when requested)
    if (showChannelPopup) {
        val currentActiveChannel = slotChannels.getOrNull(activeSlotIndex) ?: selectedPreviewChannel
        ChannelSelectionPopup(
            channels = allChannels.ifEmpty { filteredChannels },
            selectedChannelId = currentActiveChannel?.id,
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
    multiScreenCount: Int,
    isRefreshing: Boolean,
    rotationAngle: Float,
    onSelectPlaylist: (Long) -> Unit,
    onSelectMultiScreen: (Int) -> Unit,
    onReload: () -> Unit,
    onEditSource: () -> Unit,
    onCloseApp: () -> Unit
) {
    Surface(
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Baris 1: Judul Aplikasi, Indikator Saluran, dan Tombol Kontrol (Tinggi Ringkas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title & Count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(LiveRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "StreamSync",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = TextWhite
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (channelCount > 0) "($channelCount Saluran)" else "(Memuat...)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Action Buttons: Edit/Kelola Playlist, Reload, Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEditSource,
                        modifier = Modifier.size(26.dp).testTag("action_edit_source_button")
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Kelola Playlist",
                            tint = TextWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onReload,
                        enabled = !isRefreshing,
                        modifier = Modifier.size(26.dp).testTag("action_reload_button")
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = if (isRefreshing) NeonCyan else TextWhite,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(if (isRefreshing) rotationAngle else 0f)
                        )
                    }

                    IconButton(
                        onClick = onCloseApp,
                        modifier = Modifier.size(26.dp).testTag("action_close_app_button")
                    ) {
                        Icon(
                            Icons.Filled.PowerSettingsNew,
                            contentDescription = "Tutup",
                            tint = LiveRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Baris 2: Ditempatkan di bawah judul (Info Sumber Playlist di kiri, Multi Layar di kanan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Playlist Source Selector (Mode Tanda Ikon Berurutan PL 1, PL 2, PL 3...)
                LazyRow(
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        val isAll = selectedPlaylistId == 0L
                        FilterChip(
                            selected = isAll,
                            onClick = { onSelectPlaylist(0L) },
                            modifier = Modifier.height(24.dp),
                            label = {
                                Text(
                                    "Semua",
                                    fontSize = 8.5.sp,
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
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    itemsIndexed(playlists, key = { _, pl -> pl.id }) { index, source ->
                        val isSelected = selectedPlaylistId == source.id
                        val plNumber = index + 1
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPlaylist(source.id) },
                            modifier = Modifier.height(24.dp),
                            label = {
                                Text(
                                    "PL $plNumber",
                                    fontSize = 8.5.sp,
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
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                // Fasilitas Multi Layar: 1, 2, 4, 6, 8 ditempatkan rapi di kanan
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .height(24.dp)
                        .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GridView,
                        contentDescription = "Multi Layar",
                        tint = NeonCyan,
                        modifier = Modifier.size(11.dp)
                    )
                    listOf(1, 2, 4, 6, 8).forEach { count ->
                        val isChosen = multiScreenCount == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isChosen) NeonCyan else Color.Transparent)
                                .clickable { onSelectMultiScreen(count) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${count}L",
                                color = if (isChosen) Color.Black else TextWhite,
                                fontSize = 8.5.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LivePreviewPlayerSection(
    multiScreenCount: Int,
    activeSlotIndex: Int,
    slotChannels: List<Channel?>,
    activeChannel: Channel?,
    isPlaying: Boolean,
    isLoading: Boolean,
    isMuted: Boolean,
    errorMessage: String?,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    isLive: Boolean,
    playerManager: com.example.player.PlayerManager,
    onSelectSlot: (Int) -> Unit,
    onOpenChannelPickerForSlot: (Int) -> Unit,
    onPreviousChannel: (Int) -> Unit,
    onNextChannel: (Int) -> Unit,
    onOpenAudioSettings: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onSeekToFraction: (Float) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onFullScreen: (Channel) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // Multi-screen Grid Container
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .testTag("live_preview_player_section")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (multiScreenCount) {
                    1 -> {
                        // Single Full Screen Slot
                        PlayerSlotView(
                            slotIndex = 0,
                            channel = slotChannels.getOrNull(0) ?: activeChannel,
                            isActive = activeSlotIndex == 0,
                            isMuted = isMuted,
                            playerManager = playerManager,
                            onSelectSlot = { onSelectSlot(0) },
                            onOpenChannelPicker = { onOpenChannelPickerForSlot(0) },
                            onPreviousChannel = onPreviousChannel,
                            onNextChannel = onNextChannel,
                            onFullScreen = onFullScreen,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        // 2 Screens: Tampilan grid 2 baris (vertically stacked: atas dan bawah) baik mode portrait maupun landscape
                        Column(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PlayerSlotView(
                                slotIndex = 0,
                                channel = slotChannels.getOrNull(0) ?: activeChannel,
                                isActive = activeSlotIndex == 0,
                                isMuted = isMuted,
                                playerManager = playerManager,
                                onSelectSlot = { onSelectSlot(0) },
                                onOpenChannelPicker = { onOpenChannelPickerForSlot(0) },
                                onPreviousChannel = onPreviousChannel,
                                onNextChannel = onNextChannel,
                                onFullScreen = onFullScreen,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            PlayerSlotView(
                                slotIndex = 1,
                                channel = slotChannels.getOrNull(1),
                                isActive = activeSlotIndex == 1,
                                isMuted = isMuted,
                                playerManager = playerManager,
                                onSelectSlot = { onSelectSlot(1) },
                                onOpenChannelPicker = { onOpenChannelPickerForSlot(1) },
                                onPreviousChannel = onPreviousChannel,
                                onNextChannel = onNextChannel,
                                onFullScreen = onFullScreen,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                    }
                    4 -> {
                        // 4 Screens: 2 columns x 2 rows horizontal grid
                        Column(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PlayerSlotView(
                                    slotIndex = 0,
                                    channel = slotChannels.getOrNull(0) ?: activeChannel,
                                    isActive = activeSlotIndex == 0,
                                    isMuted = isMuted,
                                    playerManager = playerManager,
                                    onSelectSlot = { onSelectSlot(0) },
                                    onOpenChannelPicker = { onOpenChannelPickerForSlot(0) },
                                    onPreviousChannel = onPreviousChannel,
                                    onNextChannel = onNextChannel,
                                    onFullScreen = onFullScreen,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                PlayerSlotView(
                                    slotIndex = 1,
                                    channel = slotChannels.getOrNull(1),
                                    isActive = activeSlotIndex == 1,
                                    isMuted = isMuted,
                                    playerManager = playerManager,
                                    onSelectSlot = { onSelectSlot(1) },
                                    onOpenChannelPicker = { onOpenChannelPickerForSlot(1) },
                                    onPreviousChannel = onPreviousChannel,
                                    onNextChannel = onNextChannel,
                                    onFullScreen = onFullScreen,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            Row(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PlayerSlotView(
                                    slotIndex = 2,
                                    channel = slotChannels.getOrNull(2),
                                    isActive = activeSlotIndex == 2,
                                    isMuted = isMuted,
                                    playerManager = playerManager,
                                    onSelectSlot = { onSelectSlot(2) },
                                    onOpenChannelPicker = { onOpenChannelPickerForSlot(2) },
                                    onPreviousChannel = onPreviousChannel,
                                    onNextChannel = onNextChannel,
                                    onFullScreen = onFullScreen,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                PlayerSlotView(
                                    slotIndex = 3,
                                    channel = slotChannels.getOrNull(3),
                                    isActive = activeSlotIndex == 3,
                                    isMuted = isMuted,
                                    playerManager = playerManager,
                                    onSelectSlot = { onSelectSlot(3) },
                                    onOpenChannelPicker = { onOpenChannelPickerForSlot(3) },
                                    onPreviousChannel = onPreviousChannel,
                                    onNextChannel = onNextChannel,
                                    onFullScreen = onFullScreen,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                    6 -> {
                        if (isLandscape) {
                            // Mode Landscape 6 Layar: Grid 2 baris x 3 kolom
                            Column(
                                modifier = Modifier.fillMaxSize().padding(3.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Baris 1: 3 kolom (Slot 0, 1, 2)
                                Row(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    (0..2).forEach { slot ->
                                        PlayerSlotView(
                                            slotIndex = slot,
                                            channel = slotChannels.getOrNull(slot) ?: if (slot == 0) activeChannel else null,
                                            isActive = activeSlotIndex == slot,
                                            isMuted = isMuted,
                                            playerManager = playerManager,
                                            onSelectSlot = { onSelectSlot(slot) },
                                            onOpenChannelPicker = { onOpenChannelPickerForSlot(slot) },
                                            onPreviousChannel = onPreviousChannel,
                                            onNextChannel = onNextChannel,
                                            onFullScreen = onFullScreen,
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                }
                                // Baris 2: 3 kolom (Slot 3, 4, 5)
                                Row(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    (3..5).forEach { slot ->
                                        PlayerSlotView(
                                            slotIndex = slot,
                                            channel = slotChannels.getOrNull(slot),
                                            isActive = activeSlotIndex == slot,
                                            isMuted = isMuted,
                                            playerManager = playerManager,
                                            onSelectSlot = { onSelectSlot(slot) },
                                            onOpenChannelPicker = { onOpenChannelPickerForSlot(slot) },
                                            onPreviousChannel = onPreviousChannel,
                                            onNextChannel = onNextChannel,
                                            onFullScreen = onFullScreen,
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                }
                            }
                        } else {
                            // Mode Portrait 6 Layar: Grid 3 baris x 2 kolom
                            Column(
                                modifier = Modifier.fillMaxSize().padding(3.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                listOf(0..1, 2..3, 4..5).forEach { range ->
                                    Row(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        range.forEach { slot ->
                                            PlayerSlotView(
                                                slotIndex = slot,
                                                channel = slotChannels.getOrNull(slot) ?: if (slot == 0) activeChannel else null,
                                                isActive = activeSlotIndex == slot,
                                                isMuted = isMuted,
                                                playerManager = playerManager,
                                                onSelectSlot = { onSelectSlot(slot) },
                                                onOpenChannelPicker = { onOpenChannelPickerForSlot(slot) },
                                                onPreviousChannel = onPreviousChannel,
                                                onNextChannel = onNextChannel,
                                                onFullScreen = onFullScreen,
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    8 -> {
                        if (isLandscape) {
                            // Mode Landscape 8 Layar: Grid 2 baris x 4 kolom
                            Column(
                                modifier = Modifier.fillMaxSize().padding(3.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Baris 1: 4 kolom (Slot 0, 1, 2, 3)
                                Row(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    (0..3).forEach { slot ->
                                        PlayerSlotView(
                                            slotIndex = slot,
                                            channel = slotChannels.getOrNull(slot) ?: if (slot == 0) activeChannel else null,
                                            isActive = activeSlotIndex == slot,
                                            isMuted = isMuted,
                                            playerManager = playerManager,
                                            onSelectSlot = { onSelectSlot(slot) },
                                            onOpenChannelPicker = { onOpenChannelPickerForSlot(slot) },
                                            onPreviousChannel = onPreviousChannel,
                                            onNextChannel = onNextChannel,
                                            onFullScreen = onFullScreen,
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                }
                                // Baris 2: 4 kolom (Slot 4, 5, 6, 7)
                                Row(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    (4..7).forEach { slot ->
                                        PlayerSlotView(
                                            slotIndex = slot,
                                            channel = slotChannels.getOrNull(slot),
                                            isActive = activeSlotIndex == slot,
                                            isMuted = isMuted,
                                            playerManager = playerManager,
                                            onSelectSlot = { onSelectSlot(slot) },
                                            onOpenChannelPicker = { onOpenChannelPickerForSlot(slot) },
                                            onPreviousChannel = onPreviousChannel,
                                            onNextChannel = onNextChannel,
                                            onFullScreen = onFullScreen,
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                }
                            }
                        } else {
                            // Mode Portrait 8 Layar: Grid 4 baris x 2 kolom
                            Column(
                                modifier = Modifier.fillMaxSize().padding(3.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                listOf(0..1, 2..3, 4..5, 6..7).forEach { range ->
                                    Row(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        range.forEach { slot ->
                                            PlayerSlotView(
                                                slotIndex = slot,
                                                channel = slotChannels.getOrNull(slot) ?: if (slot == 0) activeChannel else null,
                                                isActive = activeSlotIndex == slot,
                                                isMuted = isMuted,
                                                playerManager = playerManager,
                                                onSelectSlot = { onSelectSlot(slot) },
                                                onOpenChannelPicker = { onOpenChannelPickerForSlot(slot) },
                                                onPreviousChannel = onPreviousChannel,
                                                onNextChannel = onNextChannel,
                                                onFullScreen = onFullScreen,
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Global Error Banner if any
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = LiveRed,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- Panel Scroll Alur Video (Interactive Seekbar & Speed Controls) ---
        Surface(
            color = DarkSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Header Bar: Titik Posisi, Slot Aktif, dan Waktu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Scroll Alur Video",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Alur Video (Layar #${activeSlotIndex + 1})",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val formattedCurrent = formatTime(currentPositionMs)
                    val formattedDuration = if (durationMs > 0) formatTime(durationMs) else if (isLive) "LIVE" else "--:--"
                    Text(
                        text = "$formattedCurrent / $formattedDuration",
                        color = NeonCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Interactive Slider / Scroll Alur Video
                val sliderValue = if (durationMs > 0) {
                    (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { fraction ->
                        onSeekToFraction(fraction)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("live_preview_timeline_slider")
                )

                // Controls Row: Mundur -10s | Play/Pause | Pilihan Kecepatan | Maju +10s
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mundur 10 detik & Play/Pause & Audio Settings & Mute
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        IconButton(
                            onClick = { onSeekRelative(-10000L) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = "Mundur 10s",
                                tint = TextWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Tombol Audio untuk memunculkan scroll pengaturan volume perangkat (0-100)
                        IconButton(
                            onClick = onOpenAudioSettings,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Pengaturan Audio (0-100)",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Nyalakan Suara" else "Bisukan",
                                tint = if (isMuted) LiveRed else TextWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Pilihan Kecepatan Putar (Perlambat / Normal / Dipercepat)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speed:",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(end = 1.dp)
                        )
                        listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            val isSelected = (playbackSpeed == speed)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isSelected) NeonCyan else DarkSurfaceElevated)
                                    .clickable { onSetPlaybackSpeed(speed) }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${speed}x",
                                    color = if (isSelected) Color.Black else TextWhite,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Maju 10 detik
                    IconButton(
                        onClick = { onSeekRelative(10000L) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Maju 10s",
                            tint = TextWhite,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerSlotView(
    slotIndex: Int,
    channel: Channel?,
    isActive: Boolean,
    isMuted: Boolean,
    playerManager: com.example.player.PlayerManager,
    onSelectSlot: () -> Unit,
    onOpenChannelPicker: () -> Unit,
    onPreviousChannel: (Int) -> Unit,
    onNextChannel: (Int) -> Unit,
    onFullScreen: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val player = remember(slotIndex) { playerManager.getPlayer(slotIndex) }

    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .border(
                width = if (isActive) 2.dp else 0.8.dp,
                color = if (isActive) NeonCyan else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slotIndex) {
                    detectTapGestures(
                        onTap = {
                            // Klik 1x di layar slot: aktifkan slot & buka kotak pop-up pemilihan channel
                            onSelectSlot()
                            onOpenChannelPicker()
                        },
                        onDoubleTap = { offset ->
                            onSelectSlot()
                            // Kuadran gesture:
                            // Y < size.height / 2 (kiri atas atau kanan atas) -> ganti channel sebelumnya (wrap-around)
                            // Y >= size.height / 2 (kiri bawah atau kanan bawah) -> ganti channel berikutnya (wrap-around)
                            val isTop = offset.y < size.height / 2
                            if (isTop) {
                                onPreviousChannel(slotIndex)
                            } else {
                                onNextChannel(slotIndex)
                            }
                        }
                    )
                }
        ) {
            if (channel != null) {
                // ExoPlayer AndroidView
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            keepScreenOn = true
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    update = { playerView ->
                        playerView.player = player
                        playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top Badge Overlay: Slot #, Channel Name, Sound indicator, Fullscreen
                // Catatan: Tanda close DIHILANGKAN agar tampilan live streaming mewah dan bersih!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isActive) NeonCyan else DarkSurfaceElevated, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "#${slotIndex + 1}",
                                color = if (isActive) Color.Black else TextWhite,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = channel.name,
                            color = TextWhite,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isActive && !isMuted) Icons.Filled.VolumeUp else Icons.Filled.VolumeMute,
                            contentDescription = if (isActive) "Audio Aktif" else "Audio Bisukan",
                            tint = if (isActive && !isMuted) NeonCyan else TextMuted,
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { onSelectSlot() }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Layar Penuh",
                            tint = TextWhite,
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { onFullScreen(channel) }
                        )
                    }
                }
            } else {
                // Empty Slot Placeholder: Pengguna wajib memilih saluran terlebih dahulu
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(if (isActive) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated, CircleShape)
                            .border(1.dp, if (isActive) NeonCyan else Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Pilih Saluran",
                            tint = if (isActive) NeonCyan else TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Layar #${slotIndex + 1}",
                        color = if (isActive) NeonCyan else TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ketuk untuk memilih saluran",
                        color = if (isActive) NeonCyan.copy(alpha = 0.85f) else TextMuted,
                        fontSize = 8.5.sp
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
