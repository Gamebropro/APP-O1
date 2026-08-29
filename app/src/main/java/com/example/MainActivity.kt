package com.example

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.security.SecurityManager
import com.example.ui.screens.MainVaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VaultBackground

val LocalSecurityFlagUpdater = compositionLocalOf<(Boolean) -> Unit> { {} }

class MainActivity : ComponentActivity() {

    private var incomingVaultUri by mutableStateOf<Uri?>(null)

    fun applyFlagSecure(enabled: Boolean) {
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure window flags are cleared on startup so browser emulators display cleanly
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        enableEdgeToEdge()

        incomingVaultUri = intent?.data

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(
                    LocalSecurityFlagUpdater provides { enabled ->
                        applyFlagSecure(enabled)
                    }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = VaultBackground
                    ) {
                        MainVaultScreen(incomingVaultUri = incomingVaultUri)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        incomingVaultUri = intent.data
    }
}
