package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ParsedQrPayload
import com.example.data.model.QrSource
import com.example.data.repository.QrHistoryRepository
import com.example.qr.QrImageDecoder
import com.example.qr.QrPayloadParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val isTorchOn: Boolean = false,
    val hasFlashUnit: Boolean = false,
    val isZoomed: Boolean = false,
    val isScanningPaused: Boolean = false,
    val isDecodingGallery: Boolean = false,
    val activeResult: ParsedQrPayload? = null,
    val eventMessage: String? = null
)

class ScanViewModel(
    private val repository: QrHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var lastDetectedValue: String? = null
    private var lastDetectedAtMs: Long = 0L

    fun onCameraInfoUpdated(hasFlash: Boolean) {
        _uiState.update { it.copy(hasFlashUnit = hasFlash) }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
    }

    fun setTorch(enabled: Boolean) {
        _uiState.update { it.copy(isTorchOn = enabled) }
    }

    fun toggleZoom() {
        _uiState.update { it.copy(isZoomed = !it.isZoomed) }
    }

    fun onBarcodeDetected(rawValue: String) {
        val currentState = _uiState.value
        if (currentState.isScanningPaused || currentState.activeResult != null) return

        val now = System.currentTimeMillis()
        if (rawValue == lastDetectedValue && now - lastDetectedAtMs < 1800L) return

        lastDetectedValue = rawValue
        lastDetectedAtMs = now
        val parsed = QrPayloadParser.parse(rawValue)

        _uiState.update {
            it.copy(
                isScanningPaused = true,
                activeResult = parsed
            )
        }

        viewModelScope.launch {
            repository.recordEntry(rawContent = rawValue, source = QrSource.SCANNED)
        }
    }

    fun decodeGalleryImage(context: Context, uri: Uri) {
        if (_uiState.value.isDecodingGallery) return
        _uiState.update { it.copy(isDecodingGallery = true) }

        viewModelScope.launch {
            val decoded = QrImageDecoder.decodeFromUri(context.applicationContext, uri)
            if (!decoded.isNullOrBlank()) {
                val parsed = QrPayloadParser.parse(decoded)
                repository.recordEntry(rawContent = decoded, source = QrSource.SCANNED)
                _uiState.update {
                    it.copy(
                        isDecodingGallery = false,
                        isScanningPaused = true,
                        activeResult = parsed
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isDecodingGallery = false,
                        eventMessage = "No QR code found in the selected image."
                    )
                }
            }
        }
    }

    fun dismissResult() {
        _uiState.update {
            it.copy(
                activeResult = null,
                isScanningPaused = false
            )
        }
    }

    fun consumeEventMessage() {
        _uiState.update { it.copy(eventMessage = null) }
    }

    companion object {
        fun factory(repository: QrHistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScanViewModel(repository) as T
                }
            }
    }
}
