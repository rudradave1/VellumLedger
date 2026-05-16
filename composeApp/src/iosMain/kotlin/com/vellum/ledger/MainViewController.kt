package com.vellum.ledger

import androidx.compose.ui.window.ComposeUIViewController
import com.vellum.ledger.di.initKoin

private val koinInitialized = lazy {
    initKoin()
}

fun MainViewController() = ComposeUIViewController { 
    val _unused = koinInitialized.value
    App() 
}
