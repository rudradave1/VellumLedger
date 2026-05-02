package com.vellum.ledger.ui.util

import androidx.compose.runtime.Composable

expect class BiometricAuthenticator {
    fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}

@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
