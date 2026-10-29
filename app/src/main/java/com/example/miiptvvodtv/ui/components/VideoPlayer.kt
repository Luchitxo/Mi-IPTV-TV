@file:OptIn(UnstableApi::class)

package com.example.miiptvvodtv.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.example.miiptvvodtv.Channel
import com.example.miiptvvodtv.ChannelAlternative
import com.example.miiptvvodtv.ui.theme.ElectricCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "VideoPlayer"

private enum class TrackStatus {
    OK, NO_VIDEO, NO_AUDIO, AUDIO_UNSUPPORTED
}

private fun checkTracks(player: ExoPlayer): TrackStatus {
    val tracks = player.currentTracks
    var hasSupportedVideo = false
    var hasSupportedAudio = false
    val audioMimeTypes = mutableListOf<String>()

    for (group in tracks.groups) {
        if (group.type == C.TRACK_TYPE_VIDEO) {
            for (i in 0 until group.length) {
                if (group.isTrackSupported(i)) {
                    hasSupportedVideo = true
                    break
                }
            }
        } else if (group.type == C.TRACK_TYPE_AUDIO) {
            for (i in 0 until group.length) {
                if (group.isTrackSupported(i)) {
                    hasSupportedAudio = true
                    val format = group.getTrackFormat(i)
                    format.sampleMimeType?.let { audioMimeTypes.add(it) }
                }
            }
        }
    }

    if (!hasSupportedVideo) {
        return TrackStatus.NO_VIDEO
    }
    if (!hasSupportedAudio) {
        return TrackStatus.NO_AUDIO
    }

    if (audioMimeTypes.isNotEmpty() && audioMimeTypes.all { it == MimeTypes.AUDIO_AC3 || it == MimeTypes.AUDIO_E_AC3 }) {
        return TrackStatus.AUDIO_UNSUPPORTED
    }

    return TrackStatus.OK
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPlayer(
    context: Context,
    videoUrl: String,
    channelName: String = "Canal en Vivo",
    channelHeaders: Map<String, String>? = null,
    channelAlternatives: List<ChannelAlternative> = emptyList(),
    channelsList: List<Channel> = emptyList(),
    currentChannelId: String = "",
    previousChannel: Channel? = null,
    onChannelChange: (Channel) -> Unit = {},
    onPlaybackStable: (String) -> Unit = {},
    onPlaybackFailed: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaybackError by remember { mutableStateOf(false) }
    var errorMessageDetails by remember { mutableStateOf("Error desconocido en la reproducción") }
    var isInactivePrompt by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var autoRetryCount by remember { mutableIntStateOf(0) }
    var alternativeIndex by remember { mutableIntStateOf(-1) }
    var currentActiveUrl by remember { mutableStateOf(videoUrl) }
    val maxAutoRetries = 4

    var isPlaying by remember { mutableStateOf(true) }
    var showControlsOverlay by remember { mutableStateOf(true) }

    val playerFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val inactiveFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentChannelIndex = remember(currentChannelId, channelsList) {
        channelsList.indexOfFirst { it.id == currentChannelId }
    }

    BackHandler {
        onBack()
    }

    LaunchedEffect(showControlsOverlay) {
        if (!showControlsOverlay) {
            delay(50)
            playerFocusRequester.requestFocus()
        }
    }

    val exoPlayer = remember(videoUrl, channelHeaders) {
        Log.i("AudioDebug", "Iniciando reproductor ExoPlayer para URL: $videoUrl")

        val defaultUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        val requestHeaders = mutableMapOf(
            "User-Agent" to defaultUserAgent
        )

        if (!channelHeaders.isNullOrEmpty()) {
            requestHeaders.putAll(channelHeaders)
        }

        val effectiveUserAgent = requestHeaders["User-Agent"] ?: defaultUserAgent

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(effectiveUserAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)
            .setDefaultRequestProperties(requestHeaders)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(12000, 40000, 2000, 4000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                setHandleAudioBecomingNoisy(true)

                val mediaItem = when {
                    videoUrl.contains(".mpd", ignoreCase = true) -> {
                        MediaItem.Builder()
                            .setUri(Uri.parse(videoUrl))
                            .setMimeType(MimeTypes.APPLICATION_MPD)
                            .build()
                    }
                    else -> {
                        MediaItem.Builder()
                            .setUri(Uri.parse(videoUrl))
                            .setMimeType(MimeTypes.APPLICATION_M3U8)
                            .build()
                    }
                }
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
    }

    val switchToNextAlternative: () -> Unit = {
        if (alternativeIndex + 1 < channelAlternatives.size) {
            alternativeIndex++
            val alt = channelAlternatives[alternativeIndex]
            currentActiveUrl = alt.url
            Log.i("Fallback", "Cambiando a alternativa index=$alternativeIndex, url=${alt.url}")
            Log.i("Preferred", "Usando URL alternativa: ${alt.url}")
            autoRetryCount = 0

            try {
                val altMediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(alt.url))
                    .setMimeType(if (alt.url.contains(".mpd", ignoreCase = true)) MimeTypes.APPLICATION_MPD else MimeTypes.APPLICATION_M3U8)
                    .build()
                exoPlayer.setMediaItem(altMediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                Log.e(TAG, "Error al cambiar a alternativa index=$alternativeIndex", e)
                onPlaybackFailed()
                isPlaybackError = true
            }
        } else {
            onPlaybackFailed()
            isPlaybackError = true
        }
    }

    // Validación de estabilidad estricta (STATE_READY && isPlaying)
    LaunchedEffect(exoPlayer, currentActiveUrl) {
        var stableTimerReached = false

        coroutineScope.launch {
            delay(15000L)
            if (!isPlaybackError) {
                stableTimerReached = true
                if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.isPlaying) {
                    Log.i("Preferred", "Reproducción estable detectada por timeout (15s) en URL: $currentActiveUrl")
                    onPlaybackStable(currentActiveUrl)
                } else {
                    Log.i("Preferred", "Timeout 15s alcanzado pero el reproductor NO está en STATE_READY/isPlaying (state=${exoPlayer.playbackState}, isPlaying=${exoPlayer.isPlaying})")
                }
            }
        }

        delay(8000L)
        if (!isPlaybackError && !stableTimerReached) {
            val status = checkTracks(exoPlayer)
            Log.i("TrackCheck", "Verificación de pistas a los 8s para URL $currentActiveUrl: $status")

            when (status) {
                TrackStatus.NO_VIDEO, TrackStatus.AUDIO_UNSUPPORTED -> {
                    Log.i("TrackCheck", "Estado $status detectado. Disparando fallback a alternativa...")
                    switchToNextAlternative()
                }
                TrackStatus.NO_AUDIO -> {
                    Log.i("TrackCheck", "Canal sin audio detectado (NO_AUDIO). Se mantiene reproduciendo.")
                    if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.isPlaying) {
                        onPlaybackStable(currentActiveUrl)
                    }
                }
                TrackStatus.OK -> {
                    Log.i("TrackCheck", "Pistas correctas (OK).")
                    if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.isPlaying) {
                        Log.i("TrackCheck", "Reproductor listo y reproduciendo. Marcando playback estable.")
                        onPlaybackStable(currentActiveUrl)
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AutoCloseableDisposable(exoPlayer)

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioGroupsCount = tracks.groups.count { it.type == C.TRACK_TYPE_AUDIO }
                val videoGroupsCount = tracks.groups.count { it.type == C.TRACK_TYPE_VIDEO }

                if (videoGroupsCount == 0) {
                    Log.i("AudioDebug", "NO VIDEO TRACKS")
                } else {
                    for ((groupIndex, group) in tracks.groups.withIndex()) {
                        if (group.type == C.TRACK_TYPE_VIDEO) {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val isSelected = group.isTrackSelected(i)
                                val mimeType = format.sampleMimeType ?: "unknown"
                                val codecs = format.codecs ?: "unknown"
                                val width = format.width
                                val height = format.height
                                val frameRate = format.frameRate
                                val bitrate = format.bitrate

                                Log.i("AudioDebug", "Video Group [$groupIndex] Track [$i]: sampleMimeType=$mimeType, codecs=$codecs, width=$width, height=$height, frameRate=$frameRate, bitrate=$bitrate, isSelected=$isSelected")
                            }
                        }
                    }
                }

                Log.i("AudioDebug", "onTracksChanged detectó $audioGroupsCount grupos de audio y $videoGroupsCount grupos de video.")

                for ((groupIndex, group) in tracks.groups.withIndex()) {
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val isSelected = group.isTrackSelected(i)
                            val mimeType = format.sampleMimeType ?: "unknown"
                            val codecs = format.codecs ?: "unknown"
                            val channelCount = format.channelCount
                            val sampleRate = format.sampleRate
                            val bitrate = format.bitrate
                            val language = format.language ?: "und"

                            Log.i("AudioDebug", "Audio Group [$groupIndex] Track [$i]: sampleMimeType=$mimeType, codecs=$codecs, channelCount=$channelCount, sampleRate=$sampleRate, bitrate=$bitrate, language=$language, isSelected=$isSelected")
                        }
                    }
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                Log.i("AudioDebug", "onAudioSessionIdChanged: audioSessionId=$audioSessionId")
            }

            // Un solo Log.e unificado en onPlayerError y sin duplicados
            override fun onPlayerError(error: PlaybackException) {
                val errorCodeName = getExoPlayerErrorName(error.errorCode)
                val errorDescription = "[$errorCodeName] ${error.localizedMessage ?: "Error de reproducción"}"
                Log.e("AudioDebug", "onPlayerError [Code: ${error.errorCode} - $errorCodeName], causeMessage=${error.cause?.message}: ${error.localizedMessage}", error)

                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    try {
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                        exoPlayer.play()
                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al intentar resincronizar BEHIND_LIVE_WINDOW", e)
                    }
                }

                val isNetworkOrDecoderError = error.errorCode in arrayOf(
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                ) || error.errorCode.toString().startsWith("20")

                if (isNetworkOrDecoderError && autoRetryCount < maxAutoRetries) {
                    autoRetryCount++
                    val backoffMillis = (autoRetryCount * 1500L).coerceAtMost(6000L)
                    coroutineScope.launch {
                        delay(backoffMillis)
                        try {
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } catch (e: Exception) {
                            Log.e(TAG, "Error en reintento de reproducción #$autoRetryCount", e)
                        }
                    }
                } else {
                    switchToNextAlternative()
                }
            }
        })
    }

    LaunchedEffect(showControlsOverlay, isPlaying) {
        if (showControlsOverlay && isPlaying && !isPlaybackError && !isInactivePrompt) {
            delay(4000L)
            showControlsOverlay = false
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        playerFocusRequester.requestFocus()
    }

    LaunchedEffect(exoPlayer) {
        var lastPosition = -1L
        var freezeCounterSeconds = 0
        var hasStartedPlaying = false

        while (isActive) {
            delay(1000L)

            if (!isPlaybackError && !isInactivePrompt) {
                val playbackState = exoPlayer.playbackState
                val currentPos = exoPlayer.currentPosition
                val isPlayerPlaying = exoPlayer.isPlaying

                if (playbackState == Player.STATE_READY) {
                    hasStartedPlaying = true
                }

                val isPlayerBuffering = playbackState == Player.STATE_BUFFERING
                val isStuckReady = playbackState == Player.STATE_READY && (currentPos == lastPosition) && isPlayerPlaying

                if (isPlayerBuffering || isStuckReady) {
                    freezeCounterSeconds++
                    val threshold = if (!hasStartedPlaying) 15 else 6

                    if (freezeCounterSeconds >= threshold) {
                        freezeCounterSeconds = 0
                        autoRetryCount++

                        if (autoRetryCount > maxAutoRetries) {
                            switchToNextAlternative()
                        } else {
                            try {
                                if (isPlayerBuffering) {
                                    exoPlayer.prepare()
                                } else {
                                    exoPlayer.seekTo(currentPos)
                                }
                                exoPlayer.play()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error durante la auto-recuperación de congelamiento", e)
                            }
                        }
                    }
                } else {
                    freezeCounterSeconds = 0
                    if (playbackState == Player.STATE_READY) {
                        autoRetryCount = 0
                    }
                }
                lastPosition = currentPos
            }
        }
    }

    LaunchedEffect(exoPlayer) {
        delay(2 * 60 * 60 * 1000L)
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isInactivePrompt = true
        }
    }

    LaunchedEffect(isPlaybackError) {
        if (isPlaybackError) {
            delay(100)
            retryFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isInactivePrompt) {
        if (isInactivePrompt) {
            delay(100)
            inactiveFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(playerFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode

                    when (keyCode) {
                        KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                            if (channelsList.isNotEmpty() && currentChannelIndex != -1) {
                                val nextIndex = if (currentChannelIndex + 1 < channelsList.size) currentChannelIndex + 1 else 0
                                onChannelChange(channelsList[nextIndex])
                                showControlsOverlay = true
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (channelsList.isNotEmpty() && currentChannelIndex != -1) {
                                val prevIndex = if (currentChannelIndex - 1 >= 0) currentChannelIndex - 1 else channelsList.size - 1
                                onChannelChange(channelsList[prevIndex])
                                showControlsOverlay = true
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_LAST_CHANNEL, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (previousChannel != null) {
                                onChannelChange(previousChannel)
                                showControlsOverlay = true
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            showControlsOverlay = true
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            showControlsOverlay = true
                            return@onKeyEvent false
                        }
                    }
                }
                false
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            }
        )

        if (isBuffering && !isPlaybackError && !isInactivePrompt) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TvCircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showControlsOverlay && !isPlaybackError && !isInactivePrompt,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "EN VIVO",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (channelsList.isNotEmpty() && currentChannelIndex != -1) {
                        Text(
                            text = "(${currentChannelIndex + 1}/${channelsList.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = if (isPlaying) "❚❚" else "▶",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                                showControlsOverlay = true
                            }
                        ) {
                            Text(if (isPlaying) "Pausar" else "Reproducir")
                        }

                        if (previousChannel != null) {
                            Button(
                                onClick = {
                                    onChannelChange(previousChannel)
                                    showControlsOverlay = true
                                }
                            ) {
                                Text("↩ Volver a ${previousChannel.name}")
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "▲ / ▼ Cambiar canal",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Button(
                            onClick = onBack
                        ) {
                            Text("Volver al Menú")
                        }
                    }
                }
            }
        }

        if (isPlaybackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .widthIn(max = 550.dp)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Transmisión No Disponible",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = errorMessageDetails,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                autoRetryCount = 0
                                isPlaybackError = false
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            },
                            modifier = Modifier.focusRequester(retryFocusRequester)
                        ) {
                            Text("Reintentar")
                        }

                        Button(onClick = onBack) {
                            Text("Volver al Menú")
                        }
                    }
                }
            }
        }

        if (isInactivePrompt && !isPlaybackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .widthIn(max = 550.dp)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "¿Sigues viendo?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Pausamos la transmisión automática para evitar el consumo innecesario de datos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                isInactivePrompt = false
                                exoPlayer.play()
                            },
                            modifier = Modifier.focusRequester(inactiveFocusRequester)
                        ) {
                            Text("Continuar Viendo")
                        }

                        Button(onClick = onBack) {
                            Text("Salir al Menú")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoCloseableDisposable(player: ExoPlayer) {
    DisposableEffect(player) {
        onDispose {
            player.stop()
            player.release()
        }
    }
}

private fun getExoPlayerErrorName(errorCode: Int): String {
    return when (errorCode) {
        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "BEHIND_LIVE_WINDOW"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "IO_NETWORK_CONNECTION_FAILED"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "IO_NETWORK_CONNECTION_TIMEOUT"
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "IO_INVALID_HTTP_CONTENT_TYPE"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "IO_BAD_HTTP_STATUS"
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "IO_FILE_NOT_FOUND"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "PARSING_CONTAINER_MALFORMED"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "PARSING_MANIFEST_MALFORMED"
        PlaybackException.ERROR_CODE_DECODING_FAILED -> "DECODING_FAILED"
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "DECODER_INIT_FAILED"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "AUDIO_TRACK_INIT_FAILED"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> "AUDIO_TRACK_WRITE_FAILED"
        else -> "ERROR_CODE_$errorCode"
    }
}
