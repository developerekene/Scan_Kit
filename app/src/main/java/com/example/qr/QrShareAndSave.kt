package com.example.qr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.data.model.ParsedQrPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object QrShareAndSave {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    suspend fun saveQrToGallery(context: Context, bitmap: Bitmap): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fileName = "ScanKit_QR_${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/ScanKit"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("Could not create MediaStore entry")

                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } ?: error("Could not open output stream")

                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    "Saved to Pictures/ScanKit"
                } else {
                    val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        ?: context.filesDir
                    val dir = File(picturesDir, "ScanKit").apply { mkdirs() }
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file.absolutePath),
                        arrayOf("image/png"),
                        null
                    )
                    "Saved QR image: ${file.name}"
                }
            }
        }

    suspend fun shareQrImage(context: Context, bitmap: Bitmap, title: String = "ScanKit QR Code") {
        val uri = withContext(Dispatchers.IO) {
            val sharedDir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
            // Clean previous temp shares
            sharedDir.listFiles()?.forEach { it.delete() }
            val file = File(sharedDir, "scankit_qr_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share QR Code").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareText(context: Context, text: String, subject: String = "QR Content") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Share QR Content").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun executePrimaryAction(context: Context, payload: ParsedQrPayload): String? {
        return when (payload) {
            is ParsedQrPayload.Url -> {
                launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(payload.normalizedUrl)))
                null
            }
            is ParsedQrPayload.Social -> {
                launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(payload.normalizedUrl)))
                null
            }
            is ParsedQrPayload.Phone -> {
                launchSafe(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${payload.phoneNumber}")))
                null
            }
            is ParsedQrPayload.Email -> {
                val uri = Uri.parse(
                    if (payload.rawValue.startsWith("mailto:", ignoreCase = true)) {
                        payload.rawValue
                    } else {
                        "mailto:${payload.address}"
                    }
                )
                launchSafe(context, Intent(Intent.ACTION_SENDTO, uri))
                null
            }
            is ParsedQrPayload.Contact -> {
                val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                    type = ContactsContract.RawContacts.CONTENT_TYPE
                    if (payload.name.isNotBlank()) {
                        putExtra(ContactsContract.Intents.Insert.NAME, payload.name)
                    }
                    if (payload.phone.isNotBlank()) {
                        putExtra(ContactsContract.Intents.Insert.PHONE, payload.phone)
                    }
                    if (payload.email.isNotBlank()) {
                        putExtra(ContactsContract.Intents.Insert.EMAIL, payload.email)
                    }
                    if (payload.organization.isNotBlank()) {
                        putExtra(ContactsContract.Intents.Insert.COMPANY, payload.organization)
                    }
                }
                launchSafe(context, intent)
                null
            }
            is ParsedQrPayload.Wifi -> {
                if (payload.password.isNotBlank()) {
                    copyToClipboard(context, "Wi-Fi Password", payload.password)
                }
                launchSafe(context, Intent(Settings.ACTION_WIFI_SETTINGS))
                if (payload.password.isNotBlank()) {
                    "Wi-Fi password copied — select \"${payload.ssid}\" in Settings"
                } else {
                    "Opening Wi-Fi Settings for \"${payload.ssid}\""
                }
            }
            is ParsedQrPayload.Text -> {
                copyToClipboard(context, "QR Text", payload.rawValue)
                "Copied text to clipboard"
            }
        }
    }

    private fun launchSafe(context: Context, intent: Intent) {
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
