/*
 * Single activity hosting the Compose UI of the Music app.
 * Installs the system splash screen and draws edge to edge.
 */
package com.music.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.music.player.core.ui.theme.MusicTheme
import com.music.player.ui.MusicApp

/** Entry activity; owns no state beyond hosting the Compose hierarchy. */
class MainActivity : ComponentActivity() {

    /** Installs the splash screen, enables edge-to-edge drawing, and sets the Compose content. */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicTheme {
                MusicApp()
            }
        }
    }
}
