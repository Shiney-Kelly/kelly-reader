package com.kelly.reader.ui.splash

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.kelly.reader.R
import com.kelly.reader.ui.theme.AppTheme
import com.kelly.reader.ui.theme.KellyReaderTheme

// the launcher art is a rounded square, about the corner radius android uses for app icons
private val ICON_SIZE = 128.dp
private val ICON_SHAPE = RoundedCornerShape(percent = 22)
private const val ICON_IN_MS = 350
private const val NAME_DELAY_MS = 400L
private const val NAME_IN_MS = 400
private const val FLIP_MS = 500
private const val HALF_FLIP_DEGREES = 90f

/**
 * The app's own splash: the icon in its original rounded square scales in, then the app name
 * fades in under it. Tapping the icon calls [onIconTap]; once [secretShown] is true the icon
 * flips over like a page to show the second artwork and a caption. On a day the reading streak
 * hits [streakMilestone] days, a line about it fades in right under the name, and the caption
 * takes the row below it. How long the splash stays is up to the caller.
 */
@Composable
fun SplashScreen(
    secretShown: Boolean,
    streakMilestone: Int?,
    onIconTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
    // 0 is the front, 180 the back; the artwork swaps at the halfway point so the back reads
    // the right way round
    val flip by animateFloatAsState(
        targetValue = if (secretShown) 180f else 0f,
        animationSpec = tween(FLIP_MS),
        label = "splash icon flip",
    )
    val showingBack = flip > HALF_FLIP_DEGREES
    val streakLine = streakMilestone?.let { streakLineRes(it) }
    val streakAlpha by animateFloatAsState(
        targetValue = if (streakLine != null && nameShown) 1f else 0f,
        animationSpec = tween(NAME_IN_MS),
        label = "splash streak alpha",
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
                painter = painterResource(
                    if (showingBack) R.drawable.splash_icon_secret else R.drawable.splash_icon,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(ICON_SIZE)
                    .scale(iconScale)
                    .alpha(iconAlpha)
                    .graphicsLayer {
                        rotationY = if (showingBack) flip - 180f else flip
                        cameraDistance = 12f * density
                    }
                    .clip(ICON_SHAPE)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onIconTap,
                    ),
            )
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(nameAlpha),
            )
            // two rows always take their space, so nothing moves when a line appears: the streak
            // line, when there is one, sits right under the name and the secret caption below it
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (streakLine != null) {
                    StreakLine(text = stringResource(streakLine), alpha = streakAlpha)
                    SecretCaption(shown = showingBack)
                } else {
                    SecretCaption(shown = showingBack)
                    StreakLine(text = "", alpha = 0f)
                }
            }
        }
    }
}

@Composable
private fun SecretCaption(shown: Boolean) {
    Text(
        text = stringResource(R.string.splash_secret),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.alpha(if (shown) 1f else 0f),
    )
}

@Composable
private fun StreakLine(text: String, alpha: Float) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.colors.accentText,
        modifier = Modifier.alpha(alpha),
    )
}

/**
 * The line for a streak of [days], or null when [days] is not a milestone.
 */
private fun streakLineRes(days: Int): Int? = when (days) {
    7 -> R.string.splash_streak_7
    30 -> R.string.splash_streak_30
    100 -> R.string.splash_streak_100
    365 -> R.string.splash_streak_365
    else -> null
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenPreview() {
    KellyReaderTheme {
        SplashScreen(secretShown = false, streakMilestone = null, onIconTap = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SplashScreenDarkPreview() {
    KellyReaderTheme {
        SplashScreen(secretShown = false, streakMilestone = null, onIconTap = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenSecretPreview() {
    KellyReaderTheme {
        SplashScreen(secretShown = true, streakMilestone = 7, onIconTap = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenStreakPreview() {
    KellyReaderTheme {
        SplashScreen(secretShown = false, streakMilestone = 30, onIconTap = {})
    }
}
