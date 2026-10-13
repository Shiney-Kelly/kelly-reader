/*
 * Root of the Compose UI: the navigation host and the three screens.
 */
package com.kelly.reader.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

import com.kelly.reader.di.appContainer
import com.kelly.reader.ui.add.AddBooksRoute
import com.kelly.reader.ui.reader.ReaderRoute
import com.kelly.reader.ui.shelf.ShelfRoute
import com.kelly.reader.ui.splash.SplashScreen

// how long the splash stays at least, so the icon and the name get their moment
private const val SPLASH_MIN_MS = 1_500L
// each tap on the icon keeps the splash around this much longer, so the taps are not a race
private const val TAP_HOLD_MS = 1_200L
// taps needed for the secret, and how long it stays once found
private const val SECRET_TAPS = 7
private const val SECRET_HOLD_MS = 3_000L
// how long the splash stays at least once a streak milestone line shows
private const val STREAK_HOLD_MS = 2_500L
// how long to wait for the launch window signal before starting the timer anyway
private const val LAUNCH_WINDOW_TIMEOUT_MS = 2_000L

/**
 * The whole app below the theme. Shows the splash until it has run its course and the start
 * route is known, then fades into the shelf or the recent book. The splash times start once
 * [launchWindowGone] says Android's own launch window has left, so the splash is seen and
 * takes taps for its whole time. It survives rotation and shows again only on a fresh start.
 */
@Composable
fun KellyReaderApp(launchWindowGone: Boolean, modifier: Modifier = Modifier) {
    val container = appContainer()
    val appViewModel: AppViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AppViewModel(container.settingsRepository, container.bookRepository) }
        },
    )
    val startRoute by appViewModel.startRoute.collectAsStateWithLifecycle()
    val streakMilestone by appViewModel.streakMilestone.collectAsStateWithLifecycle()
    var splashElapsed by rememberSaveable { mutableStateOf(false) }
    // taps on the splash icon push the end of the splash back, and enough of them show the
    // secret; both live only for this start
    var iconTaps by rememberSaveable { mutableStateOf(0) }
    var holdUntil by remember { mutableStateOf(0L) }
    var visibleSince by remember { mutableStateOf<Long?>(null) }
    val secretShown = iconTaps >= SECRET_TAPS
    val launchWindowGoneNow by rememberUpdatedState(launchWindowGone)
    LaunchedEffect(Unit) {
        if (!splashElapsed) {
            // the app draws its first frame while the system launch window is still on top and
            // takes the taps, so the timer waits for that window to leave; the timeout covers a
            // device where the signal never comes
            withFrameNanos { }
            withTimeoutOrNull(LAUNCH_WINDOW_TIMEOUT_MS) {
                snapshotFlow { launchWindowGoneNow }.first { it }
            }
            val now = System.currentTimeMillis()
            visibleSince = now
            holdUntil = maxOf(holdUntil, now + SPLASH_MIN_MS)
            while (true) {
                val remaining = holdUntil - System.currentTimeMillis()
                if (remaining <= 0) break
                delay(remaining)
            }
            splashElapsed = true
        }
    }

    // a milestone line needs a moment to be read, so it keeps the splash up a little longer,
    // counted from when the splash became visible
    LaunchedEffect(streakMilestone, visibleSince) {
        val since = visibleSince ?: return@LaunchedEffect
        if (streakMilestone != null && !splashElapsed) {
            holdUntil = maxOf(holdUntil, since + STREAK_HOLD_MS)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val route = startRoute
        Crossfade(targetState = if (splashElapsed) route else null, label = "splash") { target ->
            if (target == null) {
                SplashScreen(
                    secretShown = secretShown,
                    streakMilestone = streakMilestone,
                    onIconTap = {
                        iconTaps++
                        val hold = if (iconTaps >= SECRET_TAPS) SECRET_HOLD_MS else TAP_HOLD_MS
                        holdUntil = maxOf(holdUntil, System.currentTimeMillis() + hold)
                    },
                )
            } else {
                AppNavHost(navController = rememberNavController(), startRoute = target)
            }
        }
    }
}

/**
 * The navigation graph. [startRoute] is the shelf or a reader route for the recent book.
 */
@Composable
private fun AppNavHost(navController: NavHostController, startRoute: String) {
    NavHost(navController = navController, startDestination = startRoute) {
        composable(Routes.SHELF) {
            ShelfRoute(
                onOpenBook = { bookId -> navController.navigate(Routes.reader(bookId)) },
                onAddBooks = { navController.navigate(Routes.ADD) },
            )
        }
        composable(Routes.ADD) {
            AddBooksRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.READER) { entry ->
            val bookId = requireNotNull(entry.arguments?.getString(BOOK_ID_ARG)) {
                "reader route needs a book id"
            }
            ReaderRoute(
                bookId = bookId,
                onBackToShelf = { navController.backToShelf() },
            )
        }
    }
}

/**
 * Leaves the reader for the shelf. Pops when the shelf is below on the stack; when the reader
 * was the start destination, the shelf replaces it instead.
 */
private fun NavHostController.backToShelf() {
    if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        navigate(Routes.SHELF) {
            popUpTo(graph.id) { inclusive = true }
        }
    }
}
