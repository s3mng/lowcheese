package app.lowcheese

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.lowcheese.ui.LowcheeseApp
import app.lowcheese.ui.theme.Ink
import app.lowcheese.ui.theme.LowcheeseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Ink.toArgb()),
        )
        setContent {
            LowcheeseTheme {
                LowcheeseApp()
            }
        }
    }
}
