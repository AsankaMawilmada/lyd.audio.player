package au.com.inoaspect.lyd.audio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import au.com.inoaspect.lyd.audio.core.design.LydTheme
import au.com.inoaspect.lyd.audio.nav.LydApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LydTheme {
                LydApp()
            }
        }
    }
}
