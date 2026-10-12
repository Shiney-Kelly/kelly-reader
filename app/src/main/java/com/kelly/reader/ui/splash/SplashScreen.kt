package com.kelly.reader.ui.splash

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.kelly.reader.R
import com.kelly.reader.ui.theme.KellyReaderTheme

// the launcher art is a rounded square, about the corner radius android uses for app icons
private val ICON_SIZE = 128.dp
private val ICON_SHAPE = RoundedCornerShape(percent = 22)
private const val ICON_IN_MS = 350
private const val NAME_DELAY_MS = 400L
private const val NAME_IN_MS = 400

/**
 * The app's own splash: the icon in its original rounded square scales in, then the app name
 * fades in under it. How long it stays is up to the caller.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    var iconShown by remember { mutableStateOf(false) }
    var nameShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        iconShown = true
        delay(NAME_DELAY_MS)
        nameShown = true
    }
    val iconScale by animateFloatAsState(
        targetValue = if (iconShown) 1f else 0.8f,
        animationSpec = tween(ICON_IN_MS),
        label = "splash icon scale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (iconShown) 1f else 0f,
        animationSpec = tween(ICON_IN_MS),
        label = "splash icon alpha",
    )
    val nameAlpha by animateFloatAsState(
        targetValue = if (nameShown) 1f else 0f,
        animationSpec = tween(NAME_IN_MS),
        label = "splash name alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.splash_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(ICON_SIZE)
                    .scale(iconScale)
                    .alpha(iconAlpha)
                    .clip(ICON_SHAPE),
            )
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(nameAlpha),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenPreview() {
    KellyReaderTheme {
        SplashScreen()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SplashScreenDarkPreview() {
    KellyReaderTheme {
        SplashScreen()
    }
}
