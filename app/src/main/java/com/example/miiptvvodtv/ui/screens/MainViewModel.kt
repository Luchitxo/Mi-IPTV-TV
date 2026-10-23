package com.example.miiptvvodtv.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.miiptvvodtv.Channel
import com.example.miiptvvodtv.data.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainViewModel"

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Success(
        val channels: List<Channel>,
        val isFromCache: Boolean
    ) : MainUiState
    data class Error(val message: String) : MainUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _progressPercent = MutableStateFlow(0)
    val progressPercent: StateFlow<Int> = _progressPercent.asStateFlow()

    private val _progressMessage = MutableStateFlow("Iniciando...")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _favoriteIds = MutableStateFlow(repository.getFavorites())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        loadMediaContent()
    }

    fun loadMediaContent() {
        viewModelScope.launch {
            // 1. Stale-First: Mostrar caché local al instante si existe (0ms de espera)
            val cached = withContext(Dispatchers.IO) { repository.getCachedContent() }
            if (cached != null && !cached.channels.isNullOrEmpty()) {
                _uiState.value = MainUiState.Success(channels = cached.channels, isFromCache = true)
            } else {
                _uiState.value = MainUiState.Loading
                _progressPercent.value = 20
                _progressMessage.value = "Cargando canales..."
            }

            // 2. Revalidate en segundo plano desde GitHub
            try {
                val freshData = repository.fetchAndSaveRemoteContent { percent, status ->
                    if (_uiState.value is MainUiState.Loading) {
                        _progressPercent.value = percent
                        _progressMessage.value = status
                    }
                }
                if (!freshData.channels.isNullOrEmpty()) {
                    _uiState.value = MainUiState.Success(channels = freshData.channels, isFromCache = false)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error al refrescar contenido de la nube", e)
                if (_uiState.value is MainUiState.Loading) {
                    val errorMsg = e.localizedMessage ?: "No se pudo conectar con el servidor ni cargar datos en caché."
                    _uiState.value = MainUiState.Error("Error al cargar contenido: $errorMsg")
                }
            }
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedFavorites = repository.toggleFavorite(channelId)
            withContext(Dispatchers.Main) {
                _favoriteIds.value = updatedFavorites
            }
        }
    }

    // Tarea 2: Funciones para exponer gestión de URL preferida sin lógica de UI
    fun getPreferredUrl(channelId: String): String? {
        return repository.getPreferredUrl(channelId)
    }

    fun savePreferredUrl(channelId: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePreferredUrl(channelId, url)
        }
    }

    fun clearPreferredUrl(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearPreferredUrl(channelId)
        }
    }
}
