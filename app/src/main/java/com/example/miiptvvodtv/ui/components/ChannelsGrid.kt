package com.example.miiptvvodtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card as MobileCard
import androidx.compose.material3.CardDefaults as MobileCardDefaults
import androidx.compose.material3.Surface as MobileSurface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.example.miiptvvodtv.Channel
import com.example.miiptvvodtv.FilterConstants
import com.example.miiptvvodtv.ui.theme.ElectricCyan
import com.example.miiptvvodtv.ui.theme.GoldStar
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelsGrid(
    channels: List<Channel>,
    favoriteIds: Set<String>,
    modifier: Modifier = Modifier,
    isTv: Boolean = true,
    selectedCategory: String = FilterConstants.ALL,
    lastSelectedId: String? = null,
    focusRestoreTrigger: Int = 0,
    onChannelClick: (Channel) -> Unit,
    onLongChannelClick: ((Channel) -> Unit)? = null,
    onToggleFavorite: (String) -> Unit
) {
    val context = LocalContext.current
    val displayChannels = remember(channels) { channels }

    if (displayChannels.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay canales disponibles.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        return
    }

    val groupedChannels by remember(displayChannels) {
        derivedStateOf {
            displayChannels.groupBy { cleanCategoryName(it.category.ifEmpty { "General" }) }
        }
    }

    val expandedCategoriesState = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(lastSelectedId, focusRestoreTrigger, displayChannels) {
        if (!lastSelectedId.isNullOrEmpty()) {
            val targetChannel = displayChannels.find { it.id == lastSelectedId }
            if (targetChannel != null) {
                val categoryName = cleanCategoryName(targetChannel.category.ifEmpty { "General" })
                expandedCategoriesState[categoryName] = true
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .focusRestorer(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedChannels.forEach { (categoryName, channelList) ->
            val isOpen = expandedCategoriesState.getOrPut(categoryName) { false }

            // 1. Encabezado de la Categoría (Soporte diferenciado para TV D-pad y Móvil Táctil)
            item(key = "accordion_header_$categoryName") {
                if (isTv) {
                    Surface(
                        onClick = { expandedCategoriesState[categoryName] = !isOpen },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = ElectricCyan.copy(alpha = 0.25f)
                        ),
                        border = ClickableSurfaceDefaults.border(
                            border = Border(border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))),
                            focusedBorder = Border(border = BorderStroke(2.dp, ElectricCyan))
                        )
                    ) {
                        CategoryHeaderContent(categoryName, channelList.size, isOpen)
                    }
                } else {
                    MobileSurface(
                        onClick = { expandedCategoriesState[categoryName] = !isOpen },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
                    ) {
                        CategoryHeaderContent(categoryName, channelList.size, isOpen)
                    }
                }
            }

            // 2. Filas de Canales (Lazy Loading real integrado en LazyColumn)
            if (isOpen) {
                val channelRows = channelList.chunked(6)
                items(
                    items = channelRows,
                    key = { row -> "row_${categoryName}_${row.first().id}" }
                ) { rowChannels ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, end = 2.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowChannels.forEach { channel ->
                            Box(modifier = Modifier.weight(1f)) {
                                val isFav = favoriteIds.contains(channel.id)
                                val focusRequester = remember { FocusRequester() }
                                val isSelected = channel.id == lastSelectedId

                                LaunchedEffect(isSelected, focusRestoreTrigger, isTv) {
                                    if (isSelected && isTv) {
                                        delay(150)
                                        try {
                                            focusRequester.requestFocus()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                                if (isTv) {
                                    Card(
                                        onClick = { onChannelClick(channel) },
                                        onLongClick = { onLongChannelClick?.invoke(channel) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(105.dp)
                                            .focusRequester(focusRequester),
                                        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
                                        scale = CardDefaults.scale(focusedScale = 1.05f),
                                        border = CardDefaults.border(
                                            border = Border(border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))),
                                            focusedBorder = Border(border = BorderStroke(2.5.dp, ElectricCyan))
                                        ),
                                        colors = CardDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        ChannelCardContent(context, channel, isFav, onToggleFavorite)
                                    }
                                } else {
                                    MobileCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .combinedClickable(
                                                onClick = { onChannelClick(channel) },
                                                onLongClick = { onLongChannelClick?.invoke(channel) }
                                            ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = MobileCardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        ChannelCardContent(context, channel, isFav, onToggleFavorite)
                                    }
                                }
                            }
                        }
                        repeat(6 - rowChannels.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeaderContent(categoryName: String, count: Int, isOpen: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = categoryName.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = ElectricCyan
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "($count canales)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Text(
            text = if (isOpen) "▲ Plegar" else "▼ Desplegar",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = ElectricCyan
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelCardContent(
    context: android.content.Context,
    channel: Channel,
    isFav: Boolean,
    onToggleFavorite: (String) -> Unit
) {
    var imageFailed by remember(channel.logo) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!imageFailed && channel.logo.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(channel.logo)
                            .crossfade(150)
                            .size(160, 160)
                            .precision(Precision.INEXACT)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        onError = { imageFailed = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = channel.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = ElectricCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = if (isFav) "★" else "☆",
            fontSize = 15.sp,
            color = if (isFav) GoldStar else Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clickable { onToggleFavorite(channel.id) }
        )
    }
}

fun cleanCategoryName(name: String): String {
    return name.replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\uD83D\\uDC00-\\uD83D\\uDDFF\\uD83E\\uDD00-\\uD83E\\uDDFF]"), "").trim()
}
