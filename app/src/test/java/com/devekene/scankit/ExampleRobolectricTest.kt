package com.devekene.scankit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.devekene.scankit.data.local.ScanKitDatabase
import com.devekene.scankit.data.model.BarcodeFormatType
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
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

    @Test
    fun `app name string is ScanKit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ScanKit", appName)
    }

    @Test
    fun `wifi builder and parser round-trip with special characters`() {
        val raw = QrPayloadBuilder.buildWifi(
            ssid = "Studio;5G",
            password = "pass:word\\123",
            security = WifiSecurity.WPA,
            isHidden = true
        )
        val parsed = QrPayloadParser.parse(raw)
        assertTrue(parsed is ParsedQrPayload.Wifi)
        val wifi = parsed as ParsedQrPayload.Wifi
        assertEquals("Studio;5G", wifi.ssid)
        assertEquals("pass:word\\123", wifi.password)
        assertEquals("WPA", wifi.security)
        assertTrue(wifi.isHidden)
    }

    @Test
    fun `vcard contact builder and parser round-trip`() {
        val raw = QrPayloadBuilder.buildContactVCard(
            name = "Alex Rivera",
            phone = "+14155550199",
            email = "alex@scankit.dev",
            organization = "ScanKit Labs",
            website = "scankit.dev"
        )
        val parsed = QrPayloadParser.parse(raw)
        assertTrue(parsed is ParsedQrPayload.Contact)
        val contact = parsed as ParsedQrPayload.Contact
        assertEquals("Alex Rivera", contact.name)
        assertEquals("+14155550199", contact.phone)
        assertEquals("alex@scankit.dev", contact.email)
        assertEquals("ScanKit Labs", contact.organization)
        assertEquals("https://scankit.dev", contact.website)
    }

    @Test
    fun `social and email and phone builders parse into typed payloads`() {
        val socialRaw = QrPayloadBuilder.buildSocial(SocialPlatform.GITHUB, "@octocat")
        val socialParsed = QrPayloadParser.parse(socialRaw)
        assertTrue(socialParsed is ParsedQrPayload.Social)
        assertEquals("GitHub", (socialParsed as ParsedQrPayload.Social).platformName)
        assertEquals("https://github.com/octocat", socialParsed.normalizedUrl)

        val emailRaw = QrPayloadBuilder.buildEmail("hi@example.com", "Hello", "Test body")
        val emailParsed = QrPayloadParser.parse(emailRaw)
        assertTrue(emailParsed is ParsedQrPayload.Email)
        assertEquals("hi@example.com", (emailParsed as ParsedQrPayload.Email).address)
        assertEquals("Hello", emailParsed.subject)
        assertEquals("Test body", emailParsed.body)

        val phoneRaw = QrPayloadBuilder.buildPhone("+15550001234")
        val phoneParsed = QrPayloadParser.parse(phoneRaw)
        assertTrue(phoneParsed is ParsedQrPayload.Phone)
        assertEquals("+15550001234", (phoneParsed as ParsedQrPayload.Phone).phoneNumber)
    }

    @Test
    fun `generated styled QR bitmap decodes back to exact payload`() {
        val expected = "https://scankit.example.com/wifi?ref=studio"
        listOf(false, true).forEach { rounded ->
            val bitmap = QrCodeGenerator.generateBitmap(
                content = expected,
                sizePx = 512,
                styleConfig = QrStyleConfig(
                    foregroundColor = 0xFF1D4ED8.toInt(),
                    backgroundColor = 0xFFFFFFFF.toInt(),
                    roundedModules = rounded
                )
            )
            assertNotNull(bitmap)
            val bmp = bitmap!!
            val pixels = IntArray(bmp.width * bmp.height)
            bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            val source = RGBLuminanceSource(bmp.width, bmp.height, pixels)
            val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))
            assertEquals(expected, result.text)
        }
    }

    @Test
    fun `barcode validation enforces format rules`() {
        assertNull(QrCodeGenerator.validateBarcodeInput("5901234123457", BarcodeFormatType.EAN_13))
        assertNotNull(QrCodeGenerator.validateBarcodeInput("12345", BarcodeFormatType.EAN_13))

        assertNull(QrCodeGenerator.validateBarcodeInput("036000291452", BarcodeFormatType.UPC_A))
        assertNotNull(QrCodeGenerator.validateBarcodeInput("abc123", BarcodeFormatType.UPC_A))

        assertNull(QrCodeGenerator.validateBarcodeInput("10012345678902", BarcodeFormatType.ITF))
        assertNotNull(QrCodeGenerator.validateBarcodeInput("12345", BarcodeFormatType.ITF))

        assertNull(QrCodeGenerator.validateBarcodeInput("ITEM-409", BarcodeFormatType.CODE_39))
        assertNotNull(QrCodeGenerator.validateBarcodeInput("item_409", BarcodeFormatType.CODE_39))
    }

    @Test
    fun `generated 1D barcode bitmaps decode back cleanly`() {
        val cases = listOf(
            BarcodeFormatType.CODE_128 to "SKU-2026-001",
            BarcodeFormatType.EAN_13 to "5901234123457"
        )
        cases.forEach { (format, value) ->
            val bitmap = QrCodeGenerator.generateBarcodeBitmap(
                content = value,
                format = format,
                widthPx = 900,
                heightPx = 360
            )
            assertNotNull("Expected bitmap for $format", bitmap)
            val bmp = bitmap!!
            val pixels = IntArray(bmp.width * bmp.height)
            bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            val source = RGBLuminanceSource(bmp.width, bmp.height, pixels)
            val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))
            assertEquals(value, result.text)
        }
    }

    @Test
    fun `parseWithFormat classifies 1D barcodes into BarcodeItem`() {
        val parsed = QrPayloadParser.parseWithFormat(
            raw = "5901234123457",
            mlKitFormat = Barcode.FORMAT_EAN_13
        )
        assertTrue(parsed is ParsedQrPayload.BarcodeItem)
        val item = parsed as ParsedQrPayload.BarcodeItem
        assertEquals("5901234123457", item.rawValue)
        assertEquals("EAN-13", item.formatLabel)
        assertEquals(BarcodeFormatType.EAN_13, item.barcodeFormat)
        assertEquals(QrContentType.BARCODE, item.type)
    }

    @Test
    fun `room history repository records favorites deletes and stores barcode format`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ScanKitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = QrHistoryRepository(db.historyDao())

        val id = repo.recordEntry("https://developer.android.com", QrSource.SCANNED)
        var items = repo.allEntries.first()
        assertEquals(1, items.size)
        assertEquals("developer.android.com", items.first().displayTitle)

        val barcodeId = repo.recordEntry(
            rawContent = "5901234123457",
            source = QrSource.CREATED,
            barcodeFormatOverride = BarcodeFormatType.EAN_13
        )
        items = repo.allEntries.first()
        assertEquals(2, items.size)
        val savedBarcode = items.first { it.id == barcodeId }
        assertEquals(QrContentType.BARCODE, savedBarcode.parsedType())
        assertEquals(BarcodeFormatType.EAN_13, savedBarcode.parsedBarcodeFormat())

        repo.toggleFavorite(id, false)
        items = repo.allEntries.first()
        assertTrue(items.first { it.id == id }.isFavorite)

        repo.deleteById(id)
        repo.deleteById(barcodeId)
        items = repo.allEntries.first()
        assertTrue(items.isEmpty())
        db.close()
    }
}
