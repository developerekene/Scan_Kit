package com.devekene.scankit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devekene.scankit.data.model.QrContentType
import com.devekene.scankit.data.model.SocialPlatform
import com.devekene.scankit.data.model.WifiSecurity
import com.devekene.scankit.ui.components.qrTypeBadgeStyle
import com.devekene.scankit.ui.theme.JetBrainsMonoFamily
import com.devekene.scankit.ui.viewmodel.CreateUiState
import com.devekene.scankit.ui.viewmodel.CreateViewModel

private data class QrColorPreset(val name: String, val argb: Int)

private val qrColorPresets = listOf(
    QrColorPreset("Obsidian", 0xFF0F172A.toInt()),
    QrColorPreset("Cobalt", 0xFF1D4ED8.toInt()),
    QrColorPreset("Cyan", 0xFF0E7490.toInt()),
    QrColorPreset("Emerald", 0xFF047857.toInt()),
    QrColorPreset("Crimson", 0xFFBE123C.toInt()),
    QrColorPreset("Violet", 0xFF6D28D9.toInt())
)

@Composable
fun CreateScreen(
    viewModel: CreateViewModel,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.eventMessage) {
        uiState.eventMessage?.let { msg ->
            onShowMessage(msg)
            viewModel.consumeEventMessage()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        val isWideLayout = maxWidth >= 720.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Studio Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "QR Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Generate, style, save & share",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.encodedPayload.isNotBlank()) {
                    TextButton(
                        onClick = { viewModel.clearCurrentForm() },
                        modifier = Modifier.testTag("create_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }

            // Category selector strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(QrContentType.entries) { category ->
                    val style = qrTypeBadgeStyle(category)
                    val selected = uiState.selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = style.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isWideLayout) {
                // Two-pane studio layout for tablets/foldables
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CategoryInputForm(uiState = uiState, viewModel = viewModel)
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 380.dp)
                            .weight(0.9f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        QrPreviewAndExportCard(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSavePng = { viewModel.saveAsPng(context) },
                            onSharePng = { viewModel.shareQrPng(context) }
                        )
                    }
                }
            } else {
                // Single-column mobile layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    QrPreviewAndExportCard(
                        uiState = uiState,
                        viewModel = viewModel,
                        onSavePng = { viewModel.saveAsPng(context) },
                        onSharePng = { viewModel.shareQrPng(context) }
                    )

                    CategoryInputForm(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun QrPreviewAndExportCard(
    uiState: CreateUiState,
    viewModel: CreateViewModel,
    onSavePng: () -> Unit,
    onSharePng: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // QR Preview Box
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = if (uiState.previewBitmap != null) 4.dp else 0.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.size(216.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    val bmp = uiState.previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Generated QR code preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .testTag("create_qr_preview_image")
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Fill in details below to preview QR code",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Style customization bar: color swatches + rounded modules toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    qrColorPresets.forEach { preset ->
                        val isSelected = uiState.styleConfig.foregroundColor == preset.argb
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(preset.argb))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setForegroundColor(preset.argb) }
                                .testTag("color_swatch_${preset.name.lowercase()}")
                        )
                    }
                }

                FilterChip(
                    selected = uiState.styleConfig.roundedModules,
                    onClick = { viewModel.setRoundedModules(!uiState.styleConfig.roundedModules) },
                    label = {
                        Text(
                            text = if (uiState.styleConfig.roundedModules) "Rounded" else "Sharp",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.testTag("toggle_rounded_modules")
                )
            }

            // Export Actions
            val hasPayload = uiState.encodedPayload.isNotBlank() && !uiState.isExporting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSavePng,
                    enabled = hasPayload,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("create_save_png_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save PNG")
                }

                FilledTonalButton(
                    onClick = onSharePng,
                    enabled = hasPayload,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("create_share_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }

            OutlinedButton(
                onClick = { viewModel.saveToHistory() },
                enabled = uiState.encodedPayload.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_save_history_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save to History")
            }
        }
    }
}

@Composable
private fun CategoryInputForm(
    uiState: CreateUiState,
    viewModel: CreateViewModel
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "${uiState.selectedCategory.label} Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            when (uiState.selectedCategory) {
                QrContentType.URL -> {
                    OutlinedTextField(
                        value = uiState.urlInput,
                        onValueChange = viewModel::updateUrlInput,
                        label = { Text("Website URL") },
                        placeholder = { Text("example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_url")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf("https://", "https://www.", ".com", ".org").forEach { snippet ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val updated = if (snippet.startsWith("http")) {
                                        if (uiState.urlInput.startsWith("http")) uiState.urlInput
                                        else snippet + uiState.urlInput
                                    } else {
                                        uiState.urlInput + snippet
                                    }
                                    viewModel.updateUrlInput(updated)
                                },
                                label = { Text(snippet, fontFamily = JetBrainsMonoFamily) }
                            )
                        }
                    }
                }

                QrContentType.WIFI -> {
                    OutlinedTextField(
                        value = uiState.wifiSsid,
                        onValueChange = viewModel::updateWifiSsid,
                        label = { Text("Network Name (SSID)") },
                        placeholder = { Text("Home_5G") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_wifi_ssid")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Security",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            WifiSecurity.entries.forEachIndexed { index, sec ->
                                SegmentedButton(
                                    selected = uiState.wifiSecurity == sec,
                                    onClick = { viewModel.updateWifiSecurity(sec) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = WifiSecurity.entries.size
                                    ),
                                    modifier = Modifier.testTag("wifi_sec_${sec.name.lowercase()}")
                                ) {
                                    Text(sec.label, maxLines = 1)
                                }
                            }
                        }
                    }

                    if (uiState.wifiSecurity != WifiSecurity.OPEN) {
                        var showPass by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = uiState.wifiPassword,
                            onValueChange = viewModel::updateWifiPassword,
                            label = { Text("Wi-Fi Password") },
                            singleLine = true,
                            visualTransformation = if (showPass) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPass = !showPass }) {
                                    Icon(
                                        imageVector = if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (showPass) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_wifi_password")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hidden Network", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "SSID is not broadcast publicly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.wifiHidden,
                            onCheckedChange = viewModel::updateWifiHidden,
                            modifier = Modifier.testTag("switch_wifi_hidden")
                        )
                    }
                }

                QrContentType.CONTACT -> {
                    OutlinedTextField(
                        value = uiState.contactName,
                        onValueChange = viewModel::updateContactName,
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_contact_name")
                    )
                    OutlinedTextField(
                        value = uiState.contactPhone,
                        onValueChange = viewModel::updateContactPhone,
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_contact_phone")
                    )
                    OutlinedTextField(
                        value = uiState.contactEmail,
                        onValueChange = viewModel::updateContactEmail,
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_contact_email")
                    )
                    OutlinedTextField(
                        value = uiState.contactOrg,
                        onValueChange = viewModel::updateContactOrg,
                        label = { Text("Company / Organization") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_contact_org")
                    )
                    OutlinedTextField(
                        value = uiState.contactWebsite,
                        onValueChange = viewModel::updateContactWebsite,
                        label = { Text("Website (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_contact_website")
                    )
                }

                QrContentType.EMAIL -> {
                    OutlinedTextField(
                        value = uiState.emailAddress,
                        onValueChange = viewModel::updateEmailAddress,
                        label = { Text("Recipient Email") },
                        placeholder = { Text("hello@business.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_email_address")
                    )
                    OutlinedTextField(
                        value = uiState.emailSubject,
                        onValueChange = viewModel::updateEmailSubject,
                        label = { Text("Subject (optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_email_subject")
                    )
                    OutlinedTextField(
                        value = uiState.emailBody,
                        onValueChange = viewModel::updateEmailBody,
                        label = { Text("Message Body (optional)") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_email_body")
                    )
                }

                QrContentType.PHONE -> {
                    OutlinedTextField(
                        value = uiState.phoneInput,
                        onValueChange = viewModel::updatePhoneInput,
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1 555 019 2834") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_phone_number")
                    )
                }

                QrContentType.SOCIAL -> {
                    Text(
                        text = "Platform",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SocialPlatform.entries) { platform ->
                            FilterChip(
                                selected = uiState.socialPlatform == platform,
                                onClick = { viewModel.updateSocialPlatform(platform) },
                                label = { Text(platform.label) },
                                modifier = Modifier.testTag("social_chip_${platform.name.lowercase()}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.socialHandle,
                        onValueChange = viewModel::updateSocialHandle,
                        label = {
                            Text(
                                if (uiState.socialPlatform == SocialPlatform.CUSTOM) {
                                    "Social Profile URL"
                                } else {
                                    "${uiState.socialPlatform.label} Handle or URL"
                                }
                            )
                        },
                        placeholder = { Text(uiState.socialPlatform.placeholder) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_social_handle")
                    )

                    if (uiState.encodedPayload.isNotBlank()) {
                        Text(
                            text = "Resolves to: ${uiState.encodedPayload}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                QrContentType.TEXT -> {
                    OutlinedTextField(
                        value = uiState.textInput,
                        onValueChange = viewModel::updateTextInput,
                        label = { Text("Plain Text or Note") },
                        placeholder = { Text("Type any text, promo code, or note...") },
                        minLines = 4,
                        maxLines = 8,
                        supportingText = {
                            Text("${uiState.textInput.length} characters")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_plain_text")
                    )
                }
            }
        }
    }
}
