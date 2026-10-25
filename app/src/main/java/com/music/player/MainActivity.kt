/*
 * Single activity hosting the Compose UI of the Music app.
 * Installs the system splash screen, draws edge to edge, keeps the playback controller connected
 * while visible, and forwards "open player" requests from the media notification.
 */
package com.music.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.music.player.core.playback.PlayerController
import com.music.player.ui.MusicApp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.android.ext.android.inject

/** Entry activity; hosts the Compose hierarchy and binds the player controller to its visible lifetime. */
class MainActivity : ComponentActivity() {

    private val player: PlayerController by inject()
    private val openPlayer = Channel<Unit>(Channel.CONFLATED)

    /** Installs the splash screen, enables edge-to-edge drawing, and sets the Compose content. */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent { MusicApp(openPlayerRequests = openPlayer.receiveAsFlow()) }
    }

    /** Connects to the playback service while the UI is visible. */
    override fun onStart() {
        super.onStart()
        player.connect()
    }

    /** Releases the controller; playback continues in the service. */
    override fun onStop() {
        player.disconnect()
        super.onStop()
    }

    /** Handles a notification tap while the activity is already running. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    /** Requests the Now Playing screen when [intent] came from the media notification. */
    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_PLAYER) openPlayer.trySend(Unit)
    }

    companion object {
        /** Intent action used by the media session to open Now Playing. */
        const val ACTION_OPEN_PLAYER = "com.music.player.action.OPEN_PLAYER"
    }
}
