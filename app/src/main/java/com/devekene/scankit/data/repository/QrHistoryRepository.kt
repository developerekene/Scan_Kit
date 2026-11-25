package com.devekene.scankit.data.repository

import com.devekene.scankit.data.local.HistoryDao
import com.devekene.scankit.data.local.HistoryEntity
import com.devekene.scankit.data.model.QrSource
import com.devekene.scankit.data.model.QrStyleConfig
import com.devekene.scankit.qr.QrPayloadParser
import kotlinx.coroutines.flow.Flow

class QrHistoryRepository(private val historyDao: HistoryDao) {
    val allEntries: Flow<List<HistoryEntity>> = historyDao.observeAll()

    suspend fun recordEntry(
        rawContent: String,
        source: QrSource,
        styleConfig: QrStyleConfig = QrStyleConfig()
    ): Long {
        val parsed = QrPayloadParser.parse(rawContent)
        val entity = HistoryEntity(
            rawContent = rawContent,
            displayTitle = parsed.title,
            displaySubtitle = parsed.subtitle,
            contentType = parsed.type.name,
            source = source.name,
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
