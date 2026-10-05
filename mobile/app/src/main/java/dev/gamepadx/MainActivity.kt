package dev.gamepadx

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF76F7C4),
                    secondary = Color(0xFFFFC857),
                    background = Color(0xFF111416),
                    surface = Color(0xFF1B2022),
                    onPrimary = Color(0xFF10201A),
                    onBackground = Color(0xFFE9F0ED),
                    onSurface = Color(0xFFE9F0ED)
                )
            ) {
                ControllerScreen(viewModel())
            }
        }
    }
}
