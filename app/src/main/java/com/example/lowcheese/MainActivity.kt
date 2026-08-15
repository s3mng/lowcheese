package com.example.lowcheese

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.lowcheese.ui.LowcheeseApp
import com.example.lowcheese.ui.theme.Ink
import com.example.lowcheese.ui.theme.LowcheeseTheme

class MainActivity : ComponentActivity() {
    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedUrl = extractSharedUrl(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Ink.toArgb()),
        )
        setContent {
            LowcheeseTheme {
                LowcheeseApp(
                    sharedUrl = sharedUrl,
                    onSharedUrlConsumed = { sharedUrl = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedUrl = extractSharedUrl(intent)
    }
}

private fun extractSharedUrl(intent: Intent?): String? {
    if (intent == null) return null
    return when (intent.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        Intent.ACTION_VIEW -> intent.dataString
        else -> null
    }
}
