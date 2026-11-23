package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ParsedQrPayload
import com.example.data.model.QrContentType
import com.example.data.model.QrSource
import com.example.data.model.QrStyleConfig
import com.example.data.model.SocialPlatform
import com.example.data.model.WifiSecurity
import com.example.data.repository.QrHistoryRepository
import com.example.qr.QrCodeGenerator
import com.example.qr.QrPayloadBuilder
import com.example.qr.QrPayloadParser
import com.example.qr.QrShareAndSave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CreateUiState(
    val selectedCategory: QrContentType = QrContentType.URL,
    val urlInput: String = "",
    val textInput: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: WifiSecurity = WifiSecurity.WPA,
    val wifiHidden: Boolean = false,
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val contactOrg: String = "",
    val contactWebsite: String = "",
    val emailAddress: String = "",
    val emailSubject: String = "",
    val emailBody: String = "",
    val phoneInput: String = "",
    val socialPlatform: SocialPlatform = SocialPlatform.INSTAGRAM,
    val socialHandle: String = "",
    val styleConfig: QrStyleConfig = QrStyleConfig(),
    val encodedPayload: String = "",
    val previewBitmap: Bitmap? = null,
    val isExporting: Boolean = false,
    val eventMessage: String? = null
)

class CreateViewModel(
    private val repository: QrHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    private var previewJob: Job? = null
    private var lastSavedSignature: String? = null

    fun selectCategory(category: QrContentType) {
        updateStateAndRebuild { it.copy(selectedCategory = category) }
    }

    fun updateUrlInput(value: String) {
        updateStateAndRebuild { it.copy(urlInput = value) }
    }

    fun updateTextInput(value: String) {
        updateStateAndRebuild { it.copy(textInput = value) }
    }

    fun updateWifiSsid(value: String) {
        updateStateAndRebuild { it.copy(wifiSsid = value) }
    }

    fun updateWifiPassword(value: String) {
        updateStateAndRebuild { it.copy(wifiPassword = value) }
    }

    fun updateWifiSecurity(security: WifiSecurity) {
        updateStateAndRebuild { it.copy(wifiSecurity = security) }
    }

    fun updateWifiHidden(hidden: Boolean) {
        updateStateAndRebuild { it.copy(wifiHidden = hidden) }
    }

    fun updateContactName(value: String) {
        updateStateAndRebuild { it.copy(contactName = value) }
    }

    fun updateContactPhone(value: String) {
        updateStateAndRebuild { it.copy(contactPhone = value) }
    }

    fun updateContactEmail(value: String) {
        updateStateAndRebuild { it.copy(contactEmail = value) }
    }

    fun updateContactOrg(value: String) {
        updateStateAndRebuild { it.copy(contactOrg = value) }
    }

    fun updateContactWebsite(value: String) {
        updateStateAndRebuild { it.copy(contactWebsite = value) }
    }

    fun updateEmailAddress(value: String) {
        updateStateAndRebuild { it.copy(emailAddress = value) }
    }

    fun updateEmailSubject(value: String) {
        updateStateAndRebuild { it.copy(emailSubject = value) }
    }

    fun updateEmailBody(value: String) {
        updateStateAndRebuild { it.copy(emailBody = value) }
    }

    fun updatePhoneInput(value: String) {
        updateStateAndRebuild { it.copy(phoneInput = value) }
    }

    fun updateSocialPlatform(platform: SocialPlatform) {
        updateStateAndRebuild { it.copy(socialPlatform = platform) }
    }

    fun updateSocialHandle(value: String) {
        updateStateAndRebuild { it.copy(socialHandle = value) }
    }

    fun setForegroundColor(color: Int) {
        updateStateAndRebuild {
            it.copy(styleConfig = it.styleConfig.copy(foregroundColor = color))
        }
    }

    fun setRoundedModules(rounded: Boolean) {
        updateStateAndRebuild {
            it.copy(styleConfig = it.styleConfig.copy(roundedModules = rounded))
        }
    }

    fun setHighErrorCorrection(high: Boolean) {
        updateStateAndRebuild {
            it.copy(styleConfig = it.styleConfig.copy(errorCorrectionHigh = high))
        }
    }

    fun clearCurrentForm() {
        updateStateAndRebuild { state ->
            when (state.selectedCategory) {
                QrContentType.URL -> state.copy(urlInput = "")
                QrContentType.TEXT -> state.copy(textInput = "")
                QrContentType.WIFI -> state.copy(wifiSsid = "", wifiPassword = "", wifiHidden = false)
                QrContentType.CONTACT -> state.copy(
                    contactName = "",
                    contactPhone = "",
                    contactEmail = "",
                    contactOrg = "",
                    contactWebsite = ""
                )
                QrContentType.EMAIL -> state.copy(emailAddress = "", emailSubject = "", emailBody = "")
                QrContentType.PHONE -> state.copy(phoneInput = "")
                QrContentType.SOCIAL -> state.copy(socialHandle = "")
            }
        }
    }

    fun prefillFromRawPayload(rawContent: String) {
        when (val parsed = QrPayloadParser.parse(rawContent)) {
            is ParsedQrPayload.Url -> updateStateAndRebuild {
                it.copy(selectedCategory = QrContentType.URL, urlInput = parsed.normalizedUrl)
            }
            is ParsedQrPayload.Wifi -> updateStateAndRebuild {
                val sec = when {
                    parsed.security.equals("WEP", ignoreCase = true) -> WifiSecurity.WEP
                    parsed.security.equals("nopass", ignoreCase = true) -> WifiSecurity.OPEN
                    else -> WifiSecurity.WPA
                }
                it.copy(
                    selectedCategory = QrContentType.WIFI,
                    wifiSsid = parsed.ssid,
                    wifiPassword = parsed.password,
                    wifiSecurity = sec,
                    wifiHidden = parsed.isHidden
                )
            }
            is ParsedQrPayload.Contact -> updateStateAndRebuild {
                it.copy(
                    selectedCategory = QrContentType.CONTACT,
                    contactName = parsed.name,
                    contactPhone = parsed.phone,
                    contactEmail = parsed.email,
                    contactOrg = parsed.organization,
                    contactWebsite = parsed.website
                )
            }
            is ParsedQrPayload.Email -> updateStateAndRebuild {
                it.copy(
                    selectedCategory = QrContentType.EMAIL,
                    emailAddress = parsed.address,
                    emailSubject = parsed.subject,
                    emailBody = parsed.body
                )
            }
            is ParsedQrPayload.Phone -> updateStateAndRebuild {
                it.copy(selectedCategory = QrContentType.PHONE, phoneInput = parsed.phoneNumber)
            }
            is ParsedQrPayload.Social -> updateStateAndRebuild {
                it.copy(
                    selectedCategory = QrContentType.SOCIAL,
                    socialPlatform = SocialPlatform.CUSTOM,
                    socialHandle = parsed.normalizedUrl
                )
            }
            is ParsedQrPayload.Text -> updateStateAndRebuild {
                it.copy(selectedCategory = QrContentType.TEXT, textInput = parsed.rawValue)
            }
        }
    }

    fun saveToHistory() {
        val state = _uiState.value
        val payload = state.encodedPayload
        if (payload.isBlank()) {
            _uiState.update { it.copy(eventMessage = "Enter content to generate a QR code first.") }
            return
        }
        viewModelScope.launch {
            recordIfNotDuplicate(payload, state.styleConfig)
            _uiState.update { it.copy(eventMessage = "Saved to History") }
        }
    }

    fun saveAsPng(context: Context) {
        val state = _uiState.value
        val payload = state.encodedPayload
        if (payload.isBlank() || state.isExporting) return

        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val highResBitmap = withContext(Dispatchers.Default) {
                QrCodeGenerator.generateBitmap(
                    content = payload,
                    sizePx = 1024,
                    styleConfig = state.styleConfig
                )
            }
            if (highResBitmap == null) {
                _uiState.update {
                    it.copy(isExporting = false, eventMessage = "Could not render QR image.")
                }
                return@launch
            }

            val result = QrShareAndSave.saveQrToGallery(context.applicationContext, highResBitmap)
            recordIfNotDuplicate(payload, state.styleConfig)

            _uiState.update {
                it.copy(
                    isExporting = false,
                    eventMessage = result.getOrElse { err ->
                        err.localizedMessage ?: "Failed to save image"
                    }
                )
            }
        }
    }

    fun shareQrPng(context: Context) {
        val state = _uiState.value
        val payload = state.encodedPayload
        if (payload.isBlank() || state.isExporting) return

        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val highResBitmap = withContext(Dispatchers.Default) {
                QrCodeGenerator.generateBitmap(
                    content = payload,
                    sizePx = 1024,
                    styleConfig = state.styleConfig
                )
            }
            if (highResBitmap != null) {
                recordIfNotDuplicate(payload, state.styleConfig)
                QrShareAndSave.shareQrImage(context, highResBitmap)
            }
            _uiState.update { it.copy(isExporting = false) }
        }
    }

    fun consumeEventMessage() {
        _uiState.update { it.copy(eventMessage = null) }
    }

    private suspend fun recordIfNotDuplicate(payload: String, styleConfig: QrStyleConfig) {
        val signature = "$payload|${styleConfig.foregroundColor}|${styleConfig.roundedModules}"
        if (signature == lastSavedSignature) return
        lastSavedSignature = signature
        repository.recordEntry(
            rawContent = payload,
            source = QrSource.CREATED,
            styleConfig = styleConfig
        )
    }

    private fun updateStateAndRebuild(transform: (CreateUiState) -> CreateUiState) {
        val updated = transform(_uiState.value)
        val payload = buildPayload(updated)
        _uiState.value = updated.copy(encodedPayload = payload)
        rebuildPreview(payload, updated.styleConfig)
    }

    private fun buildPayload(state: CreateUiState): String {
        return when (state.selectedCategory) {
            QrContentType.URL -> QrPayloadBuilder.buildUrl(state.urlInput)
            QrContentType.TEXT -> state.textInput.trim()
            QrContentType.WIFI -> QrPayloadBuilder.buildWifi(
                ssid = state.wifiSsid,
                password = state.wifiPassword,
                security = state.wifiSecurity,
                isHidden = state.wifiHidden
            )
            QrContentType.CONTACT -> QrPayloadBuilder.buildContactVCard(
                name = state.contactName,
                phone = state.contactPhone,
                email = state.contactEmail,
                organization = state.contactOrg,
                website = state.contactWebsite
            )
            QrContentType.EMAIL -> QrPayloadBuilder.buildEmail(
                address = state.emailAddress,
                subject = state.emailSubject,
                body = state.emailBody
            )
            QrContentType.PHONE -> QrPayloadBuilder.buildPhone(state.phoneInput)
            QrContentType.SOCIAL -> QrPayloadBuilder.buildSocial(
                platform = state.socialPlatform,
                handleOrUrl = state.socialHandle
            )
        }
    }

    private fun rebuildPreview(payload: String, styleConfig: QrStyleConfig) {
        previewJob?.cancel()
        if (payload.isBlank()) {
            _uiState.update { it.copy(previewBitmap = null) }
            return
        }
        previewJob = viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                QrCodeGenerator.generateBitmap(
                    content = payload,
                    sizePx = 640,
                    styleConfig = styleConfig
                )
            }
            _uiState.update { it.copy(previewBitmap = bitmap) }
        }
    }

    companion object {
        fun factory(repository: QrHistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CreateViewModel(repository) as T
                }
            }
    }
}
