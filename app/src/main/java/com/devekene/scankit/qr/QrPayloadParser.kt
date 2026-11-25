package com.devekene.scankit.qr

import android.net.Uri
import com.devekene.scankit.data.model.ParsedQrPayload
import com.devekene.scankit.data.model.SocialPlatform
import com.devekene.scankit.data.model.WifiSecurity
import java.net.URLDecoder
import java.net.URLEncoder

object QrPayloadParser {

    fun parse(raw: String): ParsedQrPayload {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return ParsedQrPayload.Text(raw)
        }

        // 1. Wi-Fi: WIFI:T:WPA;S:MyNetwork;P:Secret;H:false;;
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            return parseWifi(trimmed)
        }

        // 2. Contact: vCard or MECARD
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true)) {
            return parseVCard(trimmed)
        }
        if (trimmed.startsWith("MECARD:", ignoreCase = true)) {
            return parseMeCard(trimmed)
        }

        // 3. Email: mailto: or MATMSG:
        if (trimmed.startsWith("mailto:", ignoreCase = true)) {
            return parseMailto(trimmed)
        }
        if (trimmed.startsWith("MATMSG:", ignoreCase = true)) {
            return parseMatmsg(trimmed)
        }

        // 4. Phone: tel: or plain phone number
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val number = trimmed.substringAfter(":").trim()
            return ParsedQrPayload.Phone(rawValue = trimmed, phoneNumber = number)
        }

        // 5. URL / Social Link
        if (looksLikeUrl(trimmed)) {
            val normalized = if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }
            val uri = runCatching { Uri.parse(normalized) }.getOrNull()
            val host = uri?.host?.removePrefix("www.")?.lowercase().orEmpty()
            val socialPlatform = detectSocialPlatform(host)
            return if (socialPlatform != null) {
                val path = uri?.path?.trim('/')?.ifBlank { host } ?: normalized
                ParsedQrPayload.Social(
                    rawValue = trimmed,
                    normalizedUrl = normalized,
                    platformName = socialPlatform,
                    handleOrPath = path
                )
            } else {
                ParsedQrPayload.Url(
                    rawValue = trimmed,
                    normalizedUrl = normalized,
                    host = host
                )
            }
        }

        // 6. Plain phone number heuristic (at least 7 digits, only phone chars)
        val phoneRegex = Regex("^\\+?[0-9][0-9\\s\\-().]{6,18}$")
        if (phoneRegex.matches(trimmed) && trimmed.count { it.isDigit() } >= 7) {
            return ParsedQrPayload.Phone(rawValue = trimmed, phoneNumber = trimmed)
        }

        // 7. Plain email address heuristic
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (emailRegex.matches(trimmed)) {
            return ParsedQrPayload.Email(
                rawValue = trimmed,
                address = trimmed,
                subject = "",
                body = ""
            )
        }

        return ParsedQrPayload.Text(trimmed)
    }

    private fun parseWifi(raw: String): ParsedQrPayload.Wifi {
        val body = raw.removePrefix("WIFI:").removePrefix("wifi:")
        var ssid = ""
        var password = ""
        var security = "WPA"
        var hidden = false

        val tokens = splitEscaped(body, ';')
        for (token in tokens) {
            if (token.isBlank()) continue
            val colonIdx = token.indexOf(':')
            if (colonIdx <= 0) continue
            val key = token.substring(0, colonIdx).uppercase()
            val value = unescapeWifi(token.substring(colonIdx + 1))
            when (key) {
                "S" -> ssid = value
                "P" -> password = value
                "T" -> security = value.ifBlank { "nopass" }
                "H" -> hidden = value.equals("true", ignoreCase = true)
            }
        }

        return ParsedQrPayload.Wifi(
            rawValue = raw,
            ssid = ssid,
            password = password,
            security = security,
            isHidden = hidden
        )
    }

    private fun parseVCard(raw: String): ParsedQrPayload.Contact {
        var name = ""
        var phone = ""
        var email = ""
        var org = ""
        var url = ""

        val lines = raw.replace("\r\n", "\n").lines()
        for (line in lines) {
            val trimmed = line.trim()
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex <= 0) continue
            val keyPart = trimmed.substring(0, colonIndex).uppercase()
            val value = trimmed.substring(colonIndex + 1).trim()
            when {
                keyPart == "FN" || keyPart.startsWith("FN;") -> {
                    if (name.isBlank()) name = value
                }
                keyPart == "N" || keyPart.startsWith("N;") -> {
                    if (name.isBlank()) {
                        val parts = value.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                        name = if (parts.size >= 2) "${parts[1]} ${parts[0]}" else parts.joinToString(" ")
                    }
                }
                keyPart == "TEL" || keyPart.startsWith("TEL;") -> {
                    if (phone.isBlank()) phone = value
                }
                keyPart == "EMAIL" || keyPart.startsWith("EMAIL;") -> {
                    if (email.isBlank()) email = value
                }
                keyPart == "ORG" || keyPart.startsWith("ORG;") -> {
                    if (org.isBlank()) org = value.replace(";", " ").trim()
                }
                keyPart == "URL" || keyPart.startsWith("URL;") -> {
                    if (url.isBlank()) url = value
                }
            }
        }

        return ParsedQrPayload.Contact(
            rawValue = raw,
            name = name,
            phone = phone,
            email = email,
            organization = org,
            website = url
        )
    }

    private fun parseMeCard(raw: String): ParsedQrPayload.Contact {
        val body = raw.substringAfter(":")
        var name = ""
        var phone = ""
        var email = ""
        var org = ""
        var url = ""

        for (token in splitEscaped(body, ';')) {
            val idx = token.indexOf(':')
            if (idx <= 0) continue
            val key = token.substring(0, idx).uppercase()
            val value = unescapeWifi(token.substring(idx + 1))
            when (key) {
                "N" -> name = value.replace(",", " ").trim()
                "TEL" -> if (phone.isBlank()) phone = value
                "EMAIL" -> if (email.isBlank()) email = value
                "ORG" -> if (org.isBlank()) org = value
                "URL" -> if (url.isBlank()) url = value
            }
        }
        return ParsedQrPayload.Contact(raw, name, phone, email, org, url)
    }

    private fun parseMailto(raw: String): ParsedQrPayload.Email {
        val withoutScheme = raw.substringAfter(":", "")
        val addressPart = withoutScheme.substringBefore("?").trim()
        val queryPart = withoutScheme.substringAfter("?", "")
        var subject = ""
        var body = ""

        if (queryPart.isNotEmpty()) {
            queryPart.split("&").forEach { param ->
                val k = param.substringBefore("=").lowercase()
                val v = runCatching {
                    URLDecoder.decode(param.substringAfter("=", ""), "UTF-8")
                }.getOrDefault(param.substringAfter("=", ""))
                when (k) {
                    "subject" -> subject = v
                    "body" -> body = v
                }
            }
        }
        return ParsedQrPayload.Email(raw, addressPart, subject, body)
    }

    private fun parseMatmsg(raw: String): ParsedQrPayload.Email {
        val body = raw.substringAfter(":")
        var to = ""
        var subject = ""
        var msgBody = ""
        for (token in splitEscaped(body, ';')) {
            val idx = token.indexOf(':')
            if (idx <= 0) continue
            val key = token.substring(0, idx).uppercase()
            val value = unescapeWifi(token.substring(idx + 1))
            when (key) {
                "TO" -> to = value
                "SUB" -> subject = value
                "BODY" -> msgBody = value
            }
        }
        return ParsedQrPayload.Email(raw, to, subject, msgBody)
    }

    private fun looksLikeUrl(text: String): Boolean {
        if (text.contains('\n') || text.contains(' ')) return false
        if (text.startsWith("http://", ignoreCase = true) ||
            text.startsWith("https://", ignoreCase = true)
        ) {
            return true
        }
        val domainRegex = Regex("^[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(/.*)?$")
        return domainRegex.matches(text) && !text.contains("@")
    }

    private fun detectSocialPlatform(host: String): String? {
        return when {
            host == "instagram.com" || host.endsWith(".instagram.com") -> "Instagram"
            host == "x.com" || host == "twitter.com" || host.endsWith(".twitter.com") -> "X / Twitter"
            host == "linkedin.com" || host.endsWith(".linkedin.com") -> "LinkedIn"
            host == "github.com" || host.endsWith(".github.com") -> "GitHub"
            host == "youtube.com" || host == "youtu.be" || host.endsWith(".youtube.com") -> "YouTube"
            host == "tiktok.com" || host.endsWith(".tiktok.com") -> "TikTok"
            host == "wa.me" || host == "whatsapp.com" -> "WhatsApp"
            else -> null
        }
    }

    private fun splitEscaped(input: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (ch in input) {
            if (escaped) {
                current.append('\\').append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == delimiter) {
                result.add(current.toString())
                current.clear()
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }

    private fun unescapeWifi(value: String): String {
        return value
            .replace("\\;", ";")
            .replace("\\:", ":")
            .replace("\\,", ",")
            .replace("\\\\", "\\")
    }
}

object QrPayloadBuilder {

    fun buildUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun buildWifi(
        ssid: String,
        password: String,
        security: WifiSecurity,
        isHidden: Boolean
    ): String {
        val cleanSsid = escapeWifi(ssid.trim())
        if (cleanSsid.isEmpty()) return ""
        val cleanPass = if (security == WifiSecurity.OPEN) "" else escapeWifi(password)
        val hiddenFlag = if (isHidden) "true" else "false"
        return "WIFI:T:${security.code};S:$cleanSsid;P:$cleanPass;H:$hiddenFlag;;"
    }

    fun buildContactVCard(
        name: String,
        phone: String,
        email: String,
        organization: String,
        website: String
    ): String {
        val n = name.trim()
        val p = phone.trim()
        val e = email.trim()
        val o = organization.trim()
        val w = website.trim()
        if (n.isEmpty() && p.isEmpty() && e.isEmpty() && o.isEmpty()) return ""

        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            if (n.isNotEmpty()) {
                appendLine("FN:$n")
                appendLine("N:$n;;;;")
            }
            if (o.isNotEmpty()) appendLine("ORG:$o")
            if (p.isNotEmpty()) appendLine("TEL;TYPE=CELL:$p")
            if (e.isNotEmpty()) appendLine("EMAIL:$e")
            if (w.isNotEmpty()) appendLine("URL:${buildUrl(w)}")
            append("END:VCARD")
        }
    }

    fun buildEmail(address: String, subject: String, body: String): String {
        val cleanAddress = address.trim()
        if (cleanAddress.isEmpty()) return ""
        val params = mutableListOf<String>()
        if (subject.isNotBlank()) {
            params.add("subject=${urlEncode(subject.trim())}")
        }
        if (body.isNotBlank()) {
            params.add("body=${urlEncode(body.trim())}")
        }
        return if (params.isEmpty()) {
            "mailto:$cleanAddress"
        } else {
            "mailto:$cleanAddress?${params.joinToString("&")}"
        }
    }

    fun buildPhone(phone: String): String {
        val clean = phone.trim()
        if (clean.isEmpty()) return ""
        return "tel:$clean"
    }

    fun buildSocial(platform: SocialPlatform, handleOrUrl: String): String {
        val trimmed = handleOrUrl.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        if (platform == SocialPlatform.CUSTOM) {
            return buildUrl(trimmed)
        }
        val cleanHandle = when (platform) {
            SocialPlatform.WHATSAPP -> trimmed.filter { it.isDigit() }
            else -> trimmed.removePrefix("@")
        }
        return "${platform.baseUrl}$cleanHandle"
    }

    private fun escapeWifi(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}
