package com.example.miiptvvodtv.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.miiptvvodtv.Channel
import com.example.miiptvvodtv.FilterConstants
import com.example.miiptvvodtv.R
import com.example.miiptvvodtv.ui.components.*
import com.example.miiptvvodtv.ui.theme.ElectricCyan
import com.example.miiptvvodtv.utils.DeviceUtils

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTv = remember { DeviceUtils.isTvDevice(context) }
    val viewModel = remember { MainViewModel(context.applicationContext as android.app.Application) }

    val uiState by viewModel.uiState.collectAsState()
    val progressPercent by viewModel.progressPercent.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var lastSelectedChannelId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentVideoUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var currentChannelName by rememberSaveable { mutableStateOf("Canal en Vivo") }
    var currentChannel by remember { mutableStateOf<Channel?>(null) }

    var previousChannel by remember { mutableStateOf<Channel?>(null) }

    var selectedChannelForAction by remember { mutableStateOf<Channel?>(null) }
    var focusRestoreTrigger by remember { mutableIntStateOf(0) }
    var selectedHeaders by remember { mutableStateOf<Map<String, String>?>(null) }

    BackHandler(enabled = currentVideoUrl == null && !showExitDialog && selectedChannelForAction == null) {
        showExitDialog = true
    }

    val horizontalPadding = if (isTv) 32.dp else 16.dp

    // Tarea 3: Al seleccionar canal, usar URL preferida guardada (si existe) en lugar de la principal
    val selectChannel: (Channel) -> Unit = { channel ->
        val currentChannels = (uiState as? MainUiState.Success)?.channels ?: emptyList()
        if (lastSelectedChannelId != null && lastSelectedChannelId != channel.id) {
            val lastChannel = currentChannels.find { it.id == lastSelectedChannelId }
            if (lastChannel != null) {
                previousChannel = lastChannel
            }
        }
        lastSelectedChannelId = channel.id
        currentChannel = channel
        currentChannelName = channel.name
        selectedHeaders = channel.headers

        val preferredUrl = viewModel.getPreferredUrl(channel.id)
        currentVideoUrl = if (!preferredUrl.isNullOrEmpty()) preferredUrl else channel.url
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (currentVideoUrl == null) {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher),
                                contentDescription = "Logo Mi IPTV",
                                modifier = Modifier
                                    .size(42.dp)
                                    .padding(end = 12.dp)
                            )

                            Text(
                                text = "MI ",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    letterSpacing = 2.sp
                                ),
                                color = Color.White
                            )

                            Text(
                                text = "IPTV",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    letterSpacing = 2.sp
                                ),
                                color = ElectricCyan
                            )
                        }

                        if (uiState is MainUiState.Success && (uiState as MainUiState.Success).isFromCache) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF9800), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Modo Offline (Lista Guardada)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    when (val state = uiState) {
                        is MainUiState.Loading -> {
                            LoadingView(
                                progressPercent = progressPercent,
                                message = progressMessage
                            )
                        }
                        is MainUiState.Error -> {
                            ErrorView(
                                errorMessage = state.message,
                                onRetry = { viewModel.loadMediaContent() }
                            )
                        }
                        is MainUiState.Success -> {
                            ChannelsGrid(
                                channels = state.channels,
                                favoriteIds = favoriteIds,
                                isTv = isTv,
                                selectedCategory = FilterConstants.ALL,
                                lastSelectedId = lastSelectedChannelId,
                                focusRestoreTrigger = focusRestoreTrigger,
                                onChannelClick = selectChannel,
                                onLongChannelClick = { channel ->
                                    selectedChannelForAction = channel
                                },
                                onToggleFavorite = { id ->
                                    viewModel.toggleFavorite(id)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (selectedChannelForAction != null) {
            ChannelActionDialog(
                channel = selectedChannelForAction!!,
                isFavorite = favoriteIds.contains(selectedChannelForAction!!.id),
                onPlayClick = selectChannel,
                onToggleFavorite = { id ->
                    viewModel.toggleFavorite(id)
                },
                onDismiss = {
                    selectedChannelForAction = null
                }
            )
        }

        if (currentVideoUrl != null) {
            val activeChannels = (uiState as? MainUiState.Success)?.channels ?: emptyList()

            Box(modifier = Modifier.fillMaxSize()) {
                VideoPlayer(
                    context = context,
                    videoUrl = currentVideoUrl!!,
                    channelName = currentChannelName,
                    channelHeaders = selectedHeaders,
                    channelAlternatives = currentChannel?.alternatives ?: emptyList(),
                    channelsList = activeChannels,
                    currentChannelId = lastSelectedChannelId ?: "",
                    previousChannel = previousChannel,
                    onChannelChange = selectChannel,
                    onPlaybackStable = { workingUrl ->
                        currentChannel?.let { ch ->
                            viewModel.savePreferredUrl(ch.id, workingUrl)
                        }
                    },
                    onPlaybackFailed = {
                        currentChannel?.let { ch ->
                            viewModel.clearPreferredUrl(ch.id)
                        }
                    },
                    onBack = {
                        currentVideoUrl = null
                        focusRestoreTrigger++
                    }
                )
            }
        }

        if (showExitDialog && currentVideoUrl == null && selectedChannelForAction == null) {
            ExitConfirmationDialog(
                onConfirmExit = {
                    (context as? Activity)?.finish()
                },
                onDismiss = {
                    showExitDialog = false
                }
            )
        }
    }
}
