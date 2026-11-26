package com.devekene.scankit.data.repository

import com.devekene.scankit.data.local.HistoryDao
import com.devekene.scankit.data.local.HistoryEntity
import com.devekene.scankit.data.model.BarcodeFormatType
import com.devekene.scankit.data.model.QrSource
import com.devekene.scankit.data.model.QrStyleConfig
import com.devekene.scankit.qr.QrPayloadParser
import kotlinx.coroutines.flow.Flow

class QrHistoryRepository(private val historyDao: HistoryDao) {
    val allEntries: Flow<List<HistoryEntity>> = historyDao.observeAll()

    suspend fun recordEntry(
        rawContent: String,
        source: QrSource,
        styleConfig: QrStyleConfig = QrStyleConfig(),
        mlKitFormat: Int? = null,
        barcodeFormatOverride: BarcodeFormatType? = null
    ): Long {
        val parsed = QrPayloadParser.parseWithFormat(
            raw = rawContent,
            mlKitFormat = mlKitFormat,
            barcodeFormatOverride = barcodeFormatOverride
        )
        val storedFormat = barcodeFormatOverride?.name
            ?: BarcodeFormatType.fromMlKitFormat(mlKitFormat)?.name
            ?: "QR_CODE"
        val entity = HistoryEntity(
            rawContent = rawContent,
            displayTitle = parsed.title,
            displaySubtitle = parsed.subtitle,
            contentType = parsed.type.name,
            source = source.name,
            codeFormat = storedFormat,
            foregroundColor = styleConfig.foregroundColor,
            backgroundColor = styleConfig.backgroundColor,
            roundedModules = styleConfig.roundedModules,
            timestamp = System.currentTimeMillis()
        )
        return historyDao.insert(entity)
    }

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        historyDao.updateFavorite(id, !currentFavorite)
    }

    suspend fun deleteById(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun clearAll() {
        historyDao.clearAll()
    }
}
