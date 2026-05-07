package com.vellum.ledger.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.Foundation.NSError

actual class BiometricAuthenticator {
    actual fun isAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)
    }

    actual fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val context = LAContext()
        var error: NSError? = null
        
        // Try biometrics first, fallback to device passcode
        if (context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error)) {
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthentication,
                localizedReason = title
            ) { success, evalError ->
                if (success) {
                    onSuccess()
                } else {
                    onError(evalError?.localizedDescription ?: "Authentication failed")
                }
            }
        } else {
            onError("Authentication not available")
        }
    }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    return remember { BiometricAuthenticator() }
}
