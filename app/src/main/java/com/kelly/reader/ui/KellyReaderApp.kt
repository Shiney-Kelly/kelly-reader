/*
 * Root of the Compose UI. Holds the navigation host once the screens exist.
 */
package com.kelly.reader.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

import com.kelly.reader.R
import com.kelly.reader.ui.theme.KellyReaderTheme

/**
 * The whole app below the theme. A placeholder until the navigation host lands.
 */
@Composable
fun KellyReaderApp(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KellyReaderAppPreview() {
    KellyReaderTheme {
        KellyReaderApp()
    }
}
