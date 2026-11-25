package com.devekene.scankit.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devekene.scankit.data.model.ParsedQrPayload
import com.devekene.scankit.data.model.QrStyleConfig
import com.devekene.scankit.qr.QrCodeGenerator
import com.devekene.scankit.qr.QrShareAndSave
import com.devekene.scankit.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrResultBottomSheet(
    payload: ParsedQrPayload,
    styleConfig: QrStyleConfig = QrStyleConfig(),
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onEditInGenerator: ((String) -> Unit)? = null,
    onFeedbackMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val badgeStyle = qrTypeBadgeStyle(payload.type)

    val qrBitmap by produceState<Bitmap?>(initialValue = null, payload.rawValue, styleConfig) {
        value = withContext(Dispatchers.Default) {
            QrCodeGenerator.generateBitmap(
                content = payload.rawValue,
                sizePx = 560,
                styleConfig = styleConfig
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row: Category Pill + Optional Favorite + Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeStyle.containerColor,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = badgeStyle.icon,
                            contentDescription = badgeStyle.label,
                            tint = badgeStyle.contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = badgeStyle.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = badgeStyle.contentColor
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onEditInGenerator != null) {
                        FilledTonalIconButton(
                            onClick = {
                                onEditInGenerator(payload.rawValue)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("result_edit_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Customize in QR Studio"
                            )
                        }
                    }
                    if (isFavorite != null && onToggleFavorite != null) {
                        FilledTonalIconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.testTag("result_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // QR Thumbnail + Title summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                qrBitmap?.let { bmp ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(92.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR code preview",
                            modifier = Modifier
                                .padding(6.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = payload.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = payload.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Structured fields card
            ParsedDetailsCard(
                payload = payload,
                onCopyField = { label, value ->
                    QrShareAndSave.copyToClipboard(context, label, value)
                    onFeedbackMessage("$label copied")
                }
            )

            // Primary contextual action button
            val primaryAction = primaryActionMeta(payload)
            Button(
                onClick = {
                    val feedback = QrShareAndSave.executePrimaryAction(context, payload)
                    if (!feedback.isNullOrBlank()) {
                        onFeedbackMessage(feedback)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("result_primary_action_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = primaryAction.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = primaryAction.label,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Secondary actions: Copy / Share Text / Save PNG / Share QR Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        QrShareAndSave.copyToClipboard(context, "QR Content", payload.rawValue)
                        onFeedbackMessage("Copied to clipboard")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_copy_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                FilledTonalButton(
                    onClick = {
                        QrShareAndSave.shareText(context, payload.rawValue, payload.title)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_share_text_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Text")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val exportBmp = withContext(Dispatchers.Default) {
                                QrCodeGenerator.generateBitmap(
                                    content = payload.rawValue,
                                    sizePx = 1024,
                                    styleConfig = styleConfig
                                )
                            }
                            if (exportBmp != null) {
                                val res = QrShareAndSave.saveQrToGallery(context, exportBmp)
                                onFeedbackMessage(
                                    res.getOrElse { it.localizedMessage ?: "Failed to save image" }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_save_png_button"),
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

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val exportBmp = withContext(Dispatchers.Default) {
                                QrCodeGenerator.generateBitmap(
                                    content = payload.rawValue,
                                    sizePx = 1024,
                                    styleConfig = styleConfig
                                )
                            }
                            if (exportBmp != null) {
                                QrShareAndSave.shareQrImage(context, exportBmp, payload.title)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_share_png_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share QR")
                }
            }
        }
    }
}

@Composable
private fun ParsedDetailsCard(
    payload: ParsedQrPayload,
    onCopyField: (String, String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (payload) {
                is ParsedQrPayload.Wifi -> {
                    DetailFieldRow("Network (SSID)", payload.ssid, onCopy = { onCopyField("SSID", payload.ssid) })
                    DetailFieldRow("Security", payload.security)
                    if (payload.password.isNotBlank()) {
                        var showPass by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Password",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (showPass) payload.password else "•".repeat(payload.password.length.coerceIn(6, 16)),
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row {
                                IconButton(onClick = { showPass = !showPass }) {
                                    Icon(
                                        imageVector = if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (showPass) "Hide password" else "Show password"
                                    )
                                }
                                IconButton(onClick = { onCopyField("Password", payload.password) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy password"
                                    )
                                }
                            }
                        }
                    }
                    if (payload.isHidden) {
                        DetailFieldRow("Visibility", "Hidden Network")
                    }
                }
                is ParsedQrPayload.Contact -> {
                    if (payload.name.isNotBlank()) DetailFieldRow("Name", payload.name)
                    if (payload.organization.isNotBlank()) DetailFieldRow("Organization", payload.organization)
                    if (payload.phone.isNotBlank()) DetailFieldRow("Phone", payload.phone, onCopy = { onCopyField("Phone", payload.phone) })
                    if (payload.email.isNotBlank()) DetailFieldRow("Email", payload.email, onCopy = { onCopyField("Email", payload.email) })
                    if (payload.website.isNotBlank()) DetailFieldRow("Website", payload.website)
                }
                is ParsedQrPayload.Email -> {
                    DetailFieldRow("Recipient", payload.address, onCopy = { onCopyField("Email", payload.address) })
                    if (payload.subject.isNotBlank()) DetailFieldRow("Subject", payload.subject)
                    if (payload.body.isNotBlank()) DetailFieldRow("Message", payload.body)
                }
                is ParsedQrPayload.Phone -> {
                    DetailFieldRow("Phone Number", payload.phoneNumber, onCopy = { onCopyField("Phone", payload.phoneNumber) })
                }
                is ParsedQrPayload.Social -> {
                    DetailFieldRow("Platform", payload.platformName)
                    DetailFieldRow("Profile URL", payload.normalizedUrl, isMonospace = true)
                }
                is ParsedQrPayload.Url -> {
                    DetailFieldRow("Domain", payload.host)
                    DetailFieldRow("Destination URL", payload.normalizedUrl, isMonospace = true)
                }
                is ParsedQrPayload.Text -> {
                    Text(
                        text = "Content",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = payload.rawValue,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (payload !is ParsedQrPayload.Text) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Raw Payload",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = payload.rawValue,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailFieldRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontFamily = if (isMonospace) JetBrainsMonoFamily else null,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy $label",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private data class PrimaryActionMeta(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun primaryActionMeta(payload: ParsedQrPayload): PrimaryActionMeta {
    return when (payload) {
        is ParsedQrPayload.Url -> PrimaryActionMeta("Open Website", Icons.AutoMirrored.Rounded.OpenInNew)
        is ParsedQrPayload.Social -> PrimaryActionMeta("Open ${payload.platformName}", Icons.AutoMirrored.Rounded.OpenInNew)
        is ParsedQrPayload.Wifi -> PrimaryActionMeta("Copy Password & Open Wi-Fi", Icons.Rounded.Wifi)
        is ParsedQrPayload.Contact -> PrimaryActionMeta("Save to Contacts", Icons.Rounded.PersonAdd)
        is ParsedQrPayload.Email -> PrimaryActionMeta("Compose Email", Icons.Rounded.Email)
        is ParsedQrPayload.Phone -> PrimaryActionMeta("Dial Number", Icons.Rounded.Call)
        is ParsedQrPayload.Text -> PrimaryActionMeta("Copy Text", Icons.Rounded.ContentCopy)
    }
}
