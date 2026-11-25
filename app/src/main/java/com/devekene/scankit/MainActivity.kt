package com.devekene.scankit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.devekene.scankit.ui.ScanKitRoot
import com.devekene.scankit.ui.theme.ScanKitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as ScanKitApp).historyRepository
        setContent {
            ScanKitTheme {
                ScanKitRoot(repository = repository)
            }
        }
    }
}
