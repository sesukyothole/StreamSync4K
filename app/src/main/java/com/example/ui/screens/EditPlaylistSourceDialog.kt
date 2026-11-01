package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Guaranteed horizontal and vertical centering
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .border(1.2.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Bar with Title and Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(NeonCyan.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAddingNew) "Tambah Sumber M3U Baru" else "Kelola / Edit Playlist",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .background(DarkSurfaceElevated, CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Tutup",
                                tint = TextWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = if (isAddingNew) Color.Black else ElectricBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
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

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Sumber Playlist", fontSize = 11.sp) },
                        placeholder = { Text("Contoh: PL4", fontSize = 11.sp) },
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
                        label = { Text(if (isAddingNew) "Tautan M3U Online Baru" else "Tautan Sumber M3U", fontSize = 11.sp) },
                        placeholder = { Text("https://domain.com/playlist.m3u", fontSize = 11.sp) },
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
                        text = "Source playlist akan otomatis muncul sebagai nomor (PL 1, PL 2, ...) di bawah judul.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                    }
                }
            }
        }
    }
}
