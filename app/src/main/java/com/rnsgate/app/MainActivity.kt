package com.rnsgate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rnsgate.app.ui.navigation.RnsGateRoot
import com.rnsgate.app.ui.theme.RnsGateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application.asRnsGate()
        setContent {
            // Dark theme by default for VPN-like gateway UX
            RnsGateTheme(darkTheme = true) {
                RnsGateRoot(
                    rnsNode = app.rnsNode,
                    messenger = app.messenger,
                    settingsStore = app.settingsStore
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewRoot() {
    RnsGateTheme(darkTheme = true) {
        // Preview shell only
    }
}
