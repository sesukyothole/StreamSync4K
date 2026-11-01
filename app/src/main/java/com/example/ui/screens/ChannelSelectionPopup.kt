package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Channel
import com.example.ui.theme.AccentGold4K
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun ChannelSelectionPopup(
    channels: List<Channel>,
    selectedChannelId: String? = null,
    deviceSpecSummary: String,
    onSelectChannel: (Channel) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(channels, query) {
        if (query.isBlank()) channels else channels.filter {
            it.name.contains(query, ignoreCase = true) || it.groupTitle.contains(query, ignoreCase = true)
        }
    }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // 6 columns in Portrait, 9 columns in Landscape as requested
    val gridColumns = if (isLandscape) 9 else 6

    val initialIndex = remember(filtered, selectedChannelId) {
        if (!selectedChannelId.isNullOrBlank()) {
            val found = filtered.indexOfFirst { it.id == selectedChannelId }
            if (found >= 0) found else 0
        } else 0
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)

    // Automatically scroll to the previously chosen channel so the user can easily pick the next one
    LaunchedEffect(selectedChannelId, filtered) {
        if (!selectedChannelId.isNullOrBlank()) {
            val found = filtered.indexOfFirst { it.id == selectedChannelId }
            if (found >= 0) {
                gridState.scrollToItem(found)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Ensure pop up is centered horizontally and vertically
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isLandscape) 12.dp else 6.dp,
                    vertical = if (isLandscape) 10.dp else 16.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.96f else 0.98f)
                    .fillMaxHeight(if (isLandscape) 0.92f else 0.82f)
                    .border(1.2.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Header Bar: Title, Mode Indicator & Close Button
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
                                    .size(26.dp)
                                    .background(NeonCyan.copy(alpha = 0.18f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.LiveTv,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Pilih Saluran Siaran",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = if (isLandscape) "Mode Lanskap (9 Kolom)" else "Mode Portret (6 Kolom)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = NeonCyan,
                                        fontSize = 8.5.sp
                                    )
                                )
                            }
                        }

                        // Close Button in top right
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

                    Spacer(modifier = Modifier.height(6.dp))

                    // Compact Search Input Box
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Cari siaran...", fontSize = 10.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Hapus", tint = TextMuted, modifier = Modifier.size(12.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceElevated,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (query.isNotBlank()) "Tidak ada saluran cocok dengan '$query'" else "Tidak ada saluran",
                                color = TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Grid Channel Selection: 6 columns in portrait, 9 columns in landscape
                        // Small compact boxes, adjusted logos, larger and bold channel titles
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            state = gridState,
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filtered, key = { it.id }) { channel ->
                                val isSelected = (channel.id == selectedChannelId)

                                // Compact square card with equal width and height
                                Card(
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) NeonCyan.copy(alpha = 0.22f) else DarkSurfaceElevated
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f) // Equal width and height (square)
                                        .clickable { onSelectChannel(channel) }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.6.dp,
                                            color = if (isSelected) NeonCyan else if (channel.is4k) AccentGold4K.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                ) {
                                    // Content centered horizontally and vertically
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 2.dp, vertical = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            // Channel Logo adjusted in size (compact 18.dp)
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else DarkSurfaceVariant.copy(alpha = 0.7f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!channel.logoUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(channel.logoUrl)
                                                            .crossfade(true)
                                                            .error(R.drawable.ic_app_logo)
                                                            .fallback(R.drawable.ic_app_logo)
                                                            .build(),
                                                        contentDescription = channel.name,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(1.dp)
                                                    )
                                                } else {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_app_logo),
                                                        contentDescription = "Logo Aplikasi",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(1.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(1.5.dp))

                                            // Channel Name: Made prominent, larger & bold as requested
                                            Text(
                                                text = channel.name,
                                                color = if (isSelected) NeonCyan else TextWhite,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 10.5.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Status indicator for active channel
                                            if (isSelected) {
                                                Text(
                                                    text = "● Aktif",
                                                    color = NeonCyan,
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

