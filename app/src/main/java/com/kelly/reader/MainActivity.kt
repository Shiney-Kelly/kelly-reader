package com.kelly.reader

import android.os.Build
import android.os.Bundle
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
 * the app splash when Android's own launch window has gone.
 */
class MainActivity : ComponentActivity() {

    private var launchWindowGone by mutableStateOf(false)

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
