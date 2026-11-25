package com.devekene.scankit

import android.app.Application
import com.devekene.scankit.data.local.ScanKitDatabase
import com.devekene.scankit.data.repository.QrHistoryRepository

class ScanKitApp : Application() {
    val database: ScanKitDatabase by lazy {
        ScanKitDatabase.getInstance(this)
    }

    val historyRepository: QrHistoryRepository by lazy {
        QrHistoryRepository(database.historyDao())
    }
}
