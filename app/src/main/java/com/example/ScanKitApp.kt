package com.example

import android.app.Application
import com.example.data.local.ScanKitDatabase
import com.example.data.repository.QrHistoryRepository

class ScanKitApp : Application() {
    val database: ScanKitDatabase by lazy {
        ScanKitDatabase.getInstance(this)
    }

    val historyRepository: QrHistoryRepository by lazy {
        QrHistoryRepository(database.historyDao())
    }
}
