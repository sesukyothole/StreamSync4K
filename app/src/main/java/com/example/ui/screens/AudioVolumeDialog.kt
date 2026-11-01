package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlayerManager
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.roundToInt

@Composable
fun AudioVolumeDialog(
    playerManager: PlayerManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val maxHardwareVolume = remember {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }

    // Initialize current volume percent (0..100) from hardware device stream
    var volumePercent by remember {
        val currentHwVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxHardwareVolume * 0.7f).roundToInt()
        val percent = if (maxHardwareVolume > 0) {
            ((currentHwVol.toFloat() / maxHardwareVolume.toFloat()) * 100f).coerceIn(0f, 100f)
        } else {
            70f
        }
        mutableFloatStateOf(percent)
    }

    // Function to apply volume changes to hardware device and player
    fun applyVolume(newPercent: Float) {
        val clamped = newPercent.coerceIn(0f, 100f)
        volumePercent = clamped

        // 1. Apply to Android Hardware AudioManager
        audioManager?.let { am ->
            val targetHw = ((clamped / 100f) * maxHardwareVolume).roundToInt()
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetHw, AudioManager.FLAG_SHOW_UI)
        }

        // 2. Apply to App Player Volume
        playerManager.setVolume(clamped / 100f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .border(1.2.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                volumePercent == 0f -> Icons.Filled.VolumeOff
                                volumePercent < 35f -> Icons.Filled.VolumeMute
                                volumePercent < 75f -> Icons.Filled.VolumeDown
                                else -> Icons.Filled.VolumeUp
                            },
                            contentDescription = "Audio Icon",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pengaturan Audio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Sinkron Volume Perangkat & Siaran",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = NeonCyan,
                                fontSize = 9.5.sp
                            )
                        )
                    }
                }

                // Tombol Close di kanan atas
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(DarkSurfaceElevated, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Tutup Pengaturan Audio",
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Volume Percentage Display
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${volumePercent.roundToInt()}%",
                            color = if (volumePercent == 0f) Color(0xFFFF5252) else NeonCyan,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = when {
                                volumePercent == 0f -> "0 (Mute / Senyap)"
                                volumePercent < 30f -> "Volume Rendah"
                                volumePercent < 70f -> "Volume Sedang"
                                volumePercent < 100f -> "Volume Keras"
                                else -> "100 (Keras Maksimal)"
                            },
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scroll Slider Pengaturan Audio (0 s/d 100)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeMute,
                        contentDescription = "Mute",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Slider(
                        value = volumePercent,
                        onValueChange = { applyVolume(it) },
                        valueRange = 0f..100f,
                        steps = 0,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Keras",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Preset Buttons (0%, 25%, 50%, 75%, 100%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        0 to "0 (Mute)",
                        25 to "25%",
                        50 to "50%",
                        75 to "75%",
                        100 to "100 (Keras)"
                    ).forEach { (preset, label) ->
                        val isSelected = volumePercent.roundToInt() == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else DarkSurfaceElevated)
                                .clickable { applyVolume(preset.toFloat()) }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else TextWhite,
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
