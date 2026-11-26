package com.devekene.scankit.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.devekene.scankit.data.model.QrContentType

data class QrTypeBadgeStyle(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
fun qrTypeBadgeStyle(type: QrContentType): QrTypeBadgeStyle {
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        QrContentType.URL -> QrTypeBadgeStyle(
            icon = Icons.Rounded.Language,
            label = "Website",
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer
        )
        QrContentType.WIFI -> QrTypeBadgeStyle(
            icon = Icons.Rounded.Wifi,
            label = "Wi-Fi",
            containerColor = scheme.secondaryContainer,
            contentColor = scheme.onSecondaryContainer
        )
        QrContentType.CONTACT -> QrTypeBadgeStyle(
            icon = Icons.Rounded.ContactPage,
            label = "Contact",
            containerColor = scheme.tertiaryContainer,
            contentColor = scheme.onTertiaryContainer
        )
        QrContentType.EMAIL -> QrTypeBadgeStyle(
            icon = Icons.Rounded.AlternateEmail,
            label = "Email",
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer
        )
        QrContentType.PHONE -> QrTypeBadgeStyle(
            icon = Icons.Rounded.Phone,
            label = "Phone",
            containerColor = scheme.tertiaryContainer,
            contentColor = scheme.onTertiaryContainer
        )
        QrContentType.SOCIAL -> QrTypeBadgeStyle(
            icon = Icons.Rounded.Share,
            label = "Social",
            containerColor = scheme.secondaryContainer,
            contentColor = scheme.onSecondaryContainer
        )
        QrContentType.BARCODE -> QrTypeBadgeStyle(
            icon = Icons.Rounded.ViewWeek,
            label = "Barcode",
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer
        )
        QrContentType.TEXT -> QrTypeBadgeStyle(
            icon = Icons.Rounded.Notes,
            label = "Text",
            containerColor = scheme.surfaceVariant,
            contentColor = scheme.onSurfaceVariant
        )
    }
}
