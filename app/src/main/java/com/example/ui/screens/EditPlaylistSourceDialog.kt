package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Playlist
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun EditPlaylistSourceDialog(
    playlist: Playlist,
    allPlaylists: List<Playlist>,
    onSaveNewSource: (playlistId: Long, newTitle: String, newUrl: String) -> Unit,
    onAddNewOnlineSource: (title: String, url: String) -> Unit,
    onImportLocalM3u: () -> Unit,
    onDeleteSource: (playlistId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(playlist) }
    var isAddingNew by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(playlist.title) }
    var url by remember { mutableStateOf(playlist.sourceUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isAddingNew) "Tambah Sumber M3U Baru" else "Kelola / Edit Sumber Playlist",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pilih Sumber Playlist (Angka 1, 2, 3...):",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                // List of all active playlists with numeric indicator (1, 2, 3, 4...)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    itemsIndexed(allPlaylists, key = { _, pl -> pl.id }) { index, pl ->
                        val numericId = index + 1
                        val isSelected = !isAddingNew && selectedPlaylist?.id == pl.id
                        SuggestionChip(
                            onClick = {
                                isAddingNew = false
                                selectedPlaylist = pl
                                title = pl.title
                                url = pl.sourceUrl
                            },
                            label = {
                                Text(
                                    text = "$numericId (${pl.title})",
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.Black else TextWhite,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) NeonCyan else DarkSurfaceElevated
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }

                    // Add New Online Source Chip
                    item {
                        SuggestionChip(
                            onClick = {
                                isAddingNew = true
                                selectedPlaylist = null
                                title = "PL${allPlaylists.size + 1}"
                                url = ""
                            },
                            icon = {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = if (isAddingNew) Color.Black else ElectricBlue, modifier = Modifier.size(12.dp))
                            },
                            label = {
                                Text(
                                    "+ Source Online",
                                    fontSize = 10.sp,
                                    color = if (isAddingNew) Color.Black else ElectricBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isAddingNew) ElectricBlue else DarkSurfaceElevated
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Sumber Playlist") },
                    placeholder = { Text("Contoh: PL4") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(if (isAddingNew) "Tautan M3U Online Baru" else "Tautan Sumber M3U") },
                    placeholder = { Text("https://domain.com/playlist.m3u") },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_playlist_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Import Offline M3U File Button
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onImportLocalM3u()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Impor Berkas M3U Offline (Perangkat)", fontSize = 11.sp)
                }

                // Delete Source option (if custom/additional playlist source)
                selectedPlaylist?.let { pl ->
                    val isDefaultSource = pl.isDefault || pl.id in 1L..3L
                    if (!isDefaultSource && !isAddingNew) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                onDeleteSource(pl.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = LiveRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hapus Source Tambahan Ini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Source tambahan akan otomatis muncul sebagai chip nomor (1, 2, 3, 4...) di kontrol baris atas.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.5.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isAddingNew) {
                        if (url.isNotBlank()) {
                            onAddNewOnlineSource(title, url)
                            onDismiss()
                        }
                    } else {
                        selectedPlaylist?.let { pl ->
                            if (url.isNotBlank()) {
                                onSaveNewSource(pl.id, title, url)
                                onDismiss()
                            }
                        }
                    }
                },
                enabled = url.isNotBlank() || !isAddingNew,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("confirm_edit_playlist_button")
            ) {
                Text(if (isAddingNew) "Tambah & Muat" else "Simpan ke Cache")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextMuted)
            }
        }
    )
}
