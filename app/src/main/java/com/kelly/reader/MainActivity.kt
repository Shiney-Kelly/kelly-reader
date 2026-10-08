package com.kelly.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.kelly.reader.ui.KellyReaderApp
import com.kelly.reader.ui.theme.KellyReaderTheme

/**
 * The single activity of the app. It draws edge to edge and hosts all Compose content.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KellyReaderTheme {
                KellyReaderApp()
            }
        }
    }
}
