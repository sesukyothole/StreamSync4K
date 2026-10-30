package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.VideoSize
import com.example.data.model.AspectRatioMode
import com.example.data.model.VideoTrackInfo
import com.example.ui.theme.AccentGold4K
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityResolutionSheet(
    videoSize: VideoSize,
    bitrate: Int,
    videoTracks: List<VideoTrackInfo>,
    aspectRatioMode: AspectRatioMode,
    playbackSpeed: Float,
    onSelectResolution: (maxHeight: Int) -> Unit,
    onSelectAspectRatio: (AspectRatioMode) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentGold4K.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.HighQuality,
                        contentDescription = null,
                        tint = AccentGold4K,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Kualitas Video & Resolusi 4K",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Stream Stats HUD Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Resolusi Aktif", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                        Spacer(modifier = Modifier.height(4.dp))
                        val resStr = if (videoSize.height > 0) "${videoSize.width}x${videoSize.height}" else "Menyesuaikan"
                        val is4kActive = videoSize.height >= 2160
                        Text(
                            text = resStr,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (is4kActive) AccentGold4K else NeonCyan
                            )
                        )
                        if (is4kActive) {
                            Text("4K ULTRA HD", style = MaterialTheme.typography.labelSmall.copy(color = AccentGold4K, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp))
                        }
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(DarkSurfaceElevated))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bitrate Aliran", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                        Spacer(modifier = Modifier.height(4.dp))
                        val bitrateStr = if (bitrate > 0) {
                            val mbps = bitrate / 1_000_000f
                            if (mbps >= 1f) String.format("%.1f Mbps", mbps) else "${bitrate / 1000} Kbps"
                        } else "Otomatis"
                        Text(
                            text = bitrateStr,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(DarkSurfaceElevated))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rasio Aspek", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aspectRatioMode.label.substringBefore(" "),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Resolution Presets
            Text(
                text = "Pilih Batas Resolusi Video",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            val resolutions = listOf(
                0 to "Otomatis (Adaptif hingga 4K UHD)",
                2160 to "4K UHD (2160p Ultra HD)",
                1080 to "1080p FHD (Full High Definition)",
                720 to "720p HD (High Definition)",
                480 to "480p SD (Hemat Kuota)"
            )

            resolutions.forEach { (height, label) ->
                val isSelected = (height == 0 && videoSize.height <= 0) || (height > 0 && videoSize.height == height)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectResolution(height) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (height == 2160) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentGold4K)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("4K", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) NeonCyan else TextWhite,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }

                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aspect Ratio mode
            Text(
                text = "Mode Tampilan Rasio Aspek",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AspectRatioMode.entries.forEach { mode ->
                    val isSelected = aspectRatioMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectAspectRatio(mode) },
                        label = { Text(mode.label.substringBefore(" "), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback speed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kecepatan Pemutaran",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val isSelected = playbackSpeed == speed
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSpeed(speed) },
                        label = { Text("${speed}x", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextWhite
                        )
                    )
                }
            }
        }
    }
}
