package com.kelly.reader

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import com.kelly.reader.ui.KellyReaderApp
import com.kelly.reader.ui.theme.KellyReaderTheme

/**
 * The single activity of the app. It draws edge to edge, hosts all Compose content, and tells
 * the app splash when Android's own launch window has gone. Volume keys reach the activity
 * rather than Compose, so a screen that wants them sets [volumeKeys].
 */
class MainActivity : ComponentActivity() {

    private var launchWindowGone by mutableStateOf(false)

    /**
     * Set by the screen in front while it uses the volume keys, as the reader does to turn
     * pages: called with true for volume up, false for down, and answers whether it used the
     * key. Null leaves the keys to the system.
     */
    var volumeKeys: ((up: Boolean) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // only a fresh start on Android 12 and newer has a system launch window to wait for;
        // a recreated activity, as after a rotation, and older Android have none
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            watchLaunchWindow()
        } else {
            launchWindowGone = true
        }
        enableEdgeToEdge()
        setContent {
            KellyReaderTheme {
                KellyReaderApp(launchWindowGone = launchWindowGone)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val handler = volumeKeys
        if (handler == null || !keyCode.isVolumeKey()) return super.onKeyDown(keyCode, event)
        // one turn a press: a key held down repeats, and is kept from the system all the same
        if (event.repeatCount > 0) return true
        return handler(keyCode == KeyEvent.KEYCODE_VOLUME_UP) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // the release of a key the screen took, so the system neither changes the volume nor
        // plays its sound
        if (volumeKeys != null && keyCode.isVolumeKey()) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun Int.isVolumeKey(): Boolean =
        this == KeyEvent.KEYCODE_VOLUME_UP || this == KeyEvent.KEYCODE_VOLUME_DOWN

    /**
     * Takes over the exit of the system splash, which tells the moment it leaves. It shows only
     * the background color, so it is removed at once instead of animated out.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun watchLaunchWindow() {
        splashScreen.setOnExitAnimationListener { view ->
            view.remove()
            launchWindowGone = true
        }
    }
}
