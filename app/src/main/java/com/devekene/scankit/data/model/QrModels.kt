package com.devekene.scankit.data.model

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
    TEXT("Text")
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
