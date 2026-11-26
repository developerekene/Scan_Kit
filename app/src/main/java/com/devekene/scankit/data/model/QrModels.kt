package com.devekene.scankit.data.model

import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat

enum class ScanMode {
    QR,
    BARCODE
}

enum class CreateKind(val label: String) {
    QR_CODE("QR Code"),
    BARCODE("Barcode")
}

enum class QrSource {
    SCANNED,
    CREATED
}

enum class QrContentType(val label: String) {
    URL("Website"),
    WIFI("Wi-Fi"),
    CONTACT("Contact"),
    EMAIL("Email"),
    PHONE("Phone"),
    SOCIAL("Social"),
    TEXT("Text"),
    BARCODE("Barcode")
}

enum class BarcodeFormatType(
    val label: String,
    val zxingFormat: BarcodeFormat,
    val mlKitFormat: Int,
    val hint: String,
    val placeholder: String
) {
    CODE_128(
        label = "Code 128",
        zxingFormat = BarcodeFormat.CODE_128,
        mlKitFormat = Barcode.FORMAT_CODE_128,
        hint = "Any ASCII text or digits (e.g. SKU-2026-A)",
        placeholder = "SKU-2026-001"
    ),
    EAN_13(
        label = "EAN-13",
        zxingFormat = BarcodeFormat.EAN_13,
        mlKitFormat = Barcode.FORMAT_EAN_13,
        hint = "12 or 13 digits (13th check digit auto-validated)",
        placeholder = "5901234123457"
    ),
    UPC_A(
        label = "UPC-A",
        zxingFormat = BarcodeFormat.UPC_A,
        mlKitFormat = Barcode.FORMAT_UPC_A,
        hint = "11 or 12 digits (retail product code)",
        placeholder = "036000291452"
    ),
    EAN_8(
        label = "EAN-8",
        zxingFormat = BarcodeFormat.EAN_8,
        mlKitFormat = Barcode.FORMAT_EAN_8,
        hint = "7 or 8 digits (compact retail barcode)",
        placeholder = "96385074"
    ),
    CODE_39(
        label = "Code 39",
        zxingFormat = BarcodeFormat.CODE_39,
        mlKitFormat = Barcode.FORMAT_CODE_39,
        hint = "Uppercase A-Z, 0-9, and - . $ / + % space",
        placeholder = "ITEM-409"
    ),
    ITF(
        label = "ITF-14",
        zxingFormat = BarcodeFormat.ITF,
        mlKitFormat = Barcode.FORMAT_ITF,
        hint = "Even number of digits (e.g. 10 to 14 digits)",
        placeholder = "10012345678902"
    ),
    CODABAR(
        label = "Codabar",
        zxingFormat = BarcodeFormat.CODABAR,
        mlKitFormat = Barcode.FORMAT_CODABAR,
        hint = "Digits 0-9 and - $ : / . +",
        placeholder = "A123456B"
    );

    companion object {
        fun fromMlKitFormat(format: Int?): BarcodeFormatType? {
            return entries.firstOrNull { it.mlKitFormat == format }
        }

        fun fromZxingFormat(format: BarcodeFormat?): BarcodeFormatType? {
            return entries.firstOrNull { it.zxingFormat == format }
        }

        fun fromStorageName(name: String?): BarcodeFormatType? {
            if (name.isNullOrBlank() || name == "QR_CODE") return null
            return entries.firstOrNull { it.name == name }
        }

        fun formatLabelFromMlKit(format: Int?): String {
            return when (format) {
                Barcode.FORMAT_EAN_13 -> "EAN-13"
                Barcode.FORMAT_EAN_8 -> "EAN-8"
                Barcode.FORMAT_UPC_A -> "UPC-A"
                Barcode.FORMAT_UPC_E -> "UPC-E"
                Barcode.FORMAT_CODE_128 -> "Code 128"
                Barcode.FORMAT_CODE_39 -> "Code 39"
                Barcode.FORMAT_CODE_93 -> "Code 93"
                Barcode.FORMAT_CODABAR -> "Codabar"
                Barcode.FORMAT_ITF -> "ITF"
                else -> "Barcode"
            }
        }
    }
}

enum class WifiSecurity(val code: String, val label: String) {
    WPA("WPA", "WPA / WPA2"),
    WEP("WEP", "WEP"),
    OPEN("nopass", "Open")
}

enum class SocialPlatform(
    val label: String,
    val baseUrl: String,
    val placeholder: String
) {
    INSTAGRAM("Instagram", "https://instagram.com/", "@username"),
    X_TWITTER("X / Twitter", "https://x.com/", "@handle"),
    LINKEDIN("LinkedIn", "https://linkedin.com/in/", "in-username"),
    GITHUB("GitHub", "https://github.com/", "username"),
    YOUTUBE("YouTube", "https://youtube.com/@", "channel"),
    TIKTOK("TikTok", "https://tiktok.com/@", "@username"),
    WHATSAPP("WhatsApp", "https://wa.me/", "15551234567"),
    CUSTOM("Custom Link", "https://", "https://...")
}

sealed class ParsedQrPayload {
    abstract val rawValue: String
    abstract val type: QrContentType
    abstract val title: String
    abstract val subtitle: String

    data class Url(
        override val rawValue: String,
        val normalizedUrl: String,
        val host: String
    ) : ParsedQrPayload() {
        override val type: QrContentType = QrContentType.URL
        override val title: String = host.ifBlank { "Website Link" }
        override val subtitle: String = normalizedUrl
    }

    data class Social(
        override val rawValue: String,
        val normalizedUrl: String,
        val platformName: String,
        val handleOrPath: String
    ) : ParsedQrPayload() {
        override val type: QrContentType = QrContentType.SOCIAL
        override val title: String = platformName
        override val subtitle: String = handleOrPath.ifBlank { normalizedUrl }
    }

    data class Wifi(
        override val rawValue: String,
        val ssid: String,
        val password: String,
        val security: String,
        val isHidden: Boolean
    ) : ParsedQrPayload() {
        override val type: QrContentType = QrContentType.WIFI
        override val title: String = ssid.ifBlank { "Wi-Fi Network" }
        override val subtitle: String = if (security.equals("nopass", ignoreCase = true)) {
            "Open Network"
        } else {
            "$security • Password protected"
        }
    }

    data class Contact(
        override val rawValue: String,
        val name: String,
        val phone: String,
        val email: String,
        val organization: String,
        val website: String
    ) : ParsedQrPayload() {
        override val type: QrContentType = QrContentType.CONTACT
        override val title: String = name.ifBlank { organization.ifBlank { "Contact Card" } }
        override val subtitle: String = listOf(phone, email, organization)
            .firstOrNull { it.isNotBlank() } ?: "vCard Contact"
    }

    data class Email(
        override val rawValue: String,
        val address: String,
        val subject: String,
        val body: String
    ) : ParsedQrPayload() {
        override val type: QrContentType = QrContentType.EMAIL
        override val title: String = address.ifBlank { "Email Message" }
        override val subtitle: String = subject.ifBlank { body.ifBlank { "Send Email" } }
    }

    data class Phone(
        override val rawValue: String,
        val phoneNumber: String
    ) : ParsedQrPayload() {
        override val type: QrContentType = QrContentType.PHONE
        override val title: String = phoneNumber
        override val subtitle: String = "Phone Number"
    }

    data class BarcodeItem(
        override val rawValue: String,
        val formatLabel: String,
        val barcodeFormat: BarcodeFormatType? = null
    ) : ParsedQrPayload() {
        override val type: QrContentType = QrContentType.BARCODE
        override val title: String = rawValue
        override val subtitle: String = "$formatLabel Barcode"
    }

    data class Text(
        override val rawValue: String
    ) : ParsedQrPayload() {
        override val type: QrContentType = QrContentType.TEXT
        override val title: String = rawValue.lines().firstOrNull()?.take(42)?.ifBlank { "Text Note" } ?: "Text Note"
        override val subtitle: String = rawValue
    }
}

data class QrStyleConfig(
    val foregroundColor: Int = 0xFF0F172A.toInt(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val roundedModules: Boolean = true,
    val errorCorrectionHigh: Boolean = false
)
