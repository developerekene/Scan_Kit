package com.devekene.scankit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.devekene.scankit.data.local.ScanKitDatabase
import com.devekene.scankit.data.model.ParsedQrPayload
import com.devekene.scankit.data.model.QrSource
import com.devekene.scankit.data.model.QrStyleConfig
import com.devekene.scankit.data.model.SocialPlatform
import com.devekene.scankit.data.model.WifiSecurity
import com.devekene.scankit.data.repository.QrHistoryRepository
import com.devekene.scankit.qr.QrCodeGenerator
import com.devekene.scankit.qr.QrPayloadBuilder
import com.devekene.scankit.qr.QrPayloadParser
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `room history repository records favorites and deletes entries`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ScanKitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = QrHistoryRepository(db.historyDao())

        val id = repo.recordEntry("https://developer.android.com", QrSource.SCANNED)
        var items = repo.allEntries.first()
        assertEquals(1, items.size)
        assertEquals("developer.android.com", items.first().displayTitle)

        repo.toggleFavorite(id, false)
        items = repo.allEntries.first()
        assertTrue(items.first().isFavorite)

        repo.deleteById(id)
        items = repo.allEntries.first()
        assertTrue(items.isEmpty())
        db.close()
    }
}
