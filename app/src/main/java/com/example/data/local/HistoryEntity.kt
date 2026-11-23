package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.QrContentType
import com.example.data.model.QrSource

@Entity(tableName = "qr_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawContent: String,
    val displayTitle: String,
    val displaySubtitle: String,
    val contentType: String = QrContentType.TEXT.name,
    val source: String = QrSource.SCANNED.name,
    val isFavorite: Boolean = false,
    val foregroundColor: Int = 0xFF0F172A.toInt(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val roundedModules: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun parsedType(): QrContentType =
        runCatching { QrContentType.valueOf(contentType) }.getOrDefault(QrContentType.TEXT)

    fun parsedSource(): QrSource =
        runCatching { QrSource.valueOf(source) }.getOrDefault(QrSource.SCANNED)
}
