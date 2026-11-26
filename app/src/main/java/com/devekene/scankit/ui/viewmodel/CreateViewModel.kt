package com.devekene.scankit.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devekene.scankit.data.model.BarcodeFormatType
import com.devekene.scankit.data.model.CreateKind
import com.devekene.scankit.data.model.ParsedQrPayload
import com.devekene.scankit.data.model.QrContentType
import com.devekene.scankit.data.model.QrSource
import com.devekene.scankit.data.model.QrStyleConfig
import com.devekene.scankit.data.model.SocialPlatform
import com.devekene.scankit.data.model.WifiSecurity
import com.devekene.scankit.data.repository.QrHistoryRepository
import com.devekene.scankit.qr.QrCodeGenerator
import com.devekene.scankit.qr.QrPayloadBuilder
import com.devekene.scankit.qr.QrPayloadParser
import com.devekene.scankit.qr.QrShareAndSave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CreateUiState(
    val createKind: CreateKind = CreateKind.QR_CODE,
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
    val barcodeFormat: BarcodeFormatType = BarcodeFormatType.CODE_128,
    val barcodeInput: String = "",
    val barcodeValidationError: String? = null,
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

    fun selectCreateKind(kind: CreateKind) {
        updateStateAndRebuild { state ->
            val nextCategory = if (kind == CreateKind.QR_CODE && state.selectedCategory == QrContentType.BARCODE) {
                QrContentType.URL
            } else {
                state.selectedCategory
            }
            state.copy(createKind = kind, selectedCategory = nextCategory)
        }
    }

    fun selectCategory(category: QrContentType) {
        updateStateAndRebuild { it.copy(selectedCategory = category) }
    }

    fun selectBarcodeFormat(format: BarcodeFormatType) {
        updateStateAndRebuild { it.copy(barcodeFormat = format) }
    }

    fun updateBarcodeInput(value: String) {
        updateStateAndRebuild { it.copy(barcodeInput = value) }
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

    fun clearCurrentForm() {
        updateStateAndRebuild { state ->
            if (state.createKind == CreateKind.BARCODE) {
                state.copy(barcodeInput = "")
            } else {
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
                    QrContentType.BARCODE -> state.copy(barcodeInput = "")
                }
            }
        }
    }

    fun prefillFromRawPayload(rawContent: String, barcodeFormat: BarcodeFormatType? = null) {
        if (barcodeFormat != null) {
            updateStateAndRebuild {
                it.copy(
                    createKind = CreateKind.BARCODE,
                    barcodeFormat = barcodeFormat,
                    barcodeInput = rawContent
                )
            }
            return
        }

        when (val parsed = QrPayloadParser.parse(rawContent)) {
            is ParsedQrPayload.Url -> updateStateAndRebuild {
                it.copy(
                    createKind = CreateKind.QR_CODE,
                    selectedCategory = QrContentType.URL,
                    urlInput = parsed.normalizedUrl
                )
            }
            is ParsedQrPayload.Wifi -> updateStateAndRebuild {
                val sec = when {
                    parsed.security.equals("WEP", ignoreCase = true) -> WifiSecurity.WEP
                    parsed.security.equals("nopass", ignoreCase = true) -> WifiSecurity.OPEN
                    else -> WifiSecurity.WPA
                }
                it.copy(
                    createKind = CreateKind.QR_CODE,
                    selectedCategory = QrContentType.WIFI,
                    wifiSsid = parsed.ssid,
                    wifiPassword = parsed.password,
                    wifiSecurity = sec,
                    wifiHidden = parsed.isHidden
                )
            }
            is ParsedQrPayload.Contact -> updateStateAndRebuild {
                it.copy(
                    createKind = CreateKind.QR_CODE,
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
                    createKind = CreateKind.QR_CODE,
                    selectedCategory = QrContentType.EMAIL,
                    emailAddress = parsed.address,
                    emailSubject = parsed.subject,
                    emailBody = parsed.body
                )
            }
            is ParsedQrPayload.Phone -> updateStateAndRebuild {
                it.copy(
                    createKind = CreateKind.QR_CODE,
                    selectedCategory = QrContentType.PHONE,
                    phoneInput = parsed.phoneNumber
                )
            }
            is ParsedQrPayload.Social -> updateStateAndRebuild {
                it.copy(
                    createKind = CreateKind.QR_CODE,
                    selectedCategory = QrContentType.SOCIAL,
                    socialPlatform = SocialPlatform.CUSTOM,
                    socialHandle = parsed.normalizedUrl
                )
            }
            is ParsedQrPayload.BarcodeItem -> updateStateAndRebuild {
                it.copy(
                    createKind = CreateKind.BARCODE,
                    barcodeFormat = parsed.barcodeFormat ?: BarcodeFormatType.CODE_128,
                    barcodeInput = parsed.rawValue
                )
            }
            is ParsedQrPayload.Text -> updateStateAndRebuild {
                it.copy(
                    createKind = CreateKind.QR_CODE,
                    selectedCategory = QrContentType.TEXT,
                    textInput = parsed.rawValue
                )
            }
        }
    }

    fun saveToHistory() {
        val state = _uiState.value
        val payload = state.encodedPayload
        if (payload.isBlank() || state.previewBitmap == null) {
            _uiState.update { it.copy(eventMessage = "Enter valid content to generate a code first.") }
            return
        }
        viewModelScope.launch {
            recordIfNotDuplicate(state, payload)
            _uiState.update { it.copy(eventMessage = "Saved to History") }
        }
    }

    fun saveAsPng(context: Context) {
        val state = _uiState.value
        val payload = state.encodedPayload
        if (payload.isBlank() || state.previewBitmap == null || state.isExporting) return

        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val highResBitmap = withContext(Dispatchers.Default) {
                renderHighResBitmap(state, payload)
            }
            if (highResBitmap == null) {
                _uiState.update {
                    it.copy(isExporting = false, eventMessage = "Could not render image.")
                }
                return@launch
            }

            val result = QrShareAndSave.saveQrToGallery(context.applicationContext, highResBitmap)
            recordIfNotDuplicate(state, payload)

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

    fun shareCodePng(context: Context) {
        val state = _uiState.value
        val payload = state.encodedPayload
        if (payload.isBlank() || state.previewBitmap == null || state.isExporting) return

        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val highResBitmap = withContext(Dispatchers.Default) {
                renderHighResBitmap(state, payload)
            }
            if (highResBitmap != null) {
                recordIfNotDuplicate(state, payload)
                QrShareAndSave.shareQrImage(context, highResBitmap)
            }
            _uiState.update { it.copy(isExporting = false) }
        }
    }

    fun consumeEventMessage() {
        _uiState.update { it.copy(eventMessage = null) }
    }

    private fun renderHighResBitmap(state: CreateUiState, payload: String): Bitmap? {
        return if (state.createKind == CreateKind.BARCODE) {
            QrCodeGenerator.generateBarcodeBitmap(
                content = payload,
                format = state.barcodeFormat,
                widthPx = 1200,
                heightPx = 460,
                foregroundColor = state.styleConfig.foregroundColor,
                backgroundColor = state.styleConfig.backgroundColor
            )
        } else {
            QrCodeGenerator.generateBitmap(
                content = payload,
                sizePx = 1024,
                styleConfig = state.styleConfig
            )
        }
    }

    private suspend fun recordIfNotDuplicate(state: CreateUiState, payload: String) {
        val formatKey = if (state.createKind == CreateKind.BARCODE) {
            state.barcodeFormat.name
        } else {
            "QR_CODE"
        }
        val signature = "$formatKey|$payload|${state.styleConfig.foregroundColor}|${state.styleConfig.roundedModules}"
        if (signature == lastSavedSignature) return
        lastSavedSignature = signature
        repository.recordEntry(
            rawContent = payload,
            source = QrSource.CREATED,
            styleConfig = state.styleConfig,
            barcodeFormatOverride = if (state.createKind == CreateKind.BARCODE) state.barcodeFormat else null
        )
    }

    private fun updateStateAndRebuild(transform: (CreateUiState) -> CreateUiState) {
        val updated = transform(_uiState.value)
        if (updated.createKind == CreateKind.BARCODE) {
            val trimmed = updated.barcodeInput.trim()
            val validationError = QrCodeGenerator.validateBarcodeInput(trimmed, updated.barcodeFormat)
            _uiState.value = updated.copy(
                encodedPayload = trimmed,
                barcodeValidationError = validationError
            )
            rebuildBarcodePreview(trimmed, updated.barcodeFormat, validationError, updated.styleConfig)
        } else {
            val payload = buildQrPayload(updated)
            _uiState.value = updated.copy(
                encodedPayload = payload,
                barcodeValidationError = null
            )
            rebuildQrPreview(payload, updated.styleConfig)
        }
    }

    private fun buildQrPayload(state: CreateUiState): String {
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
            QrContentType.BARCODE -> state.barcodeInput.trim()
        }
    }

    private fun rebuildQrPreview(payload: String, styleConfig: QrStyleConfig) {
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

    private fun rebuildBarcodePreview(
        payload: String,
        format: BarcodeFormatType,
        validationError: String?,
        styleConfig: QrStyleConfig
    ) {
        previewJob?.cancel()
        if (payload.isBlank() || validationError != null) {
            _uiState.update { it.copy(previewBitmap = null) }
            return
        }
        previewJob = viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                QrCodeGenerator.generateBarcodeBitmap(
                    content = payload,
                    format = format,
                    widthPx = 960,
                    heightPx = 360,
                    foregroundColor = styleConfig.foregroundColor,
                    backgroundColor = styleConfig.backgroundColor
                )
            }
            _uiState.update {
                val finalErr = if (bitmap == null) {
                    "Value is not valid for ${format.label} (check checksum or characters)"
                } else {
                    null
                }
                it.copy(previewBitmap = bitmap, barcodeValidationError = finalErr)
            }
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
