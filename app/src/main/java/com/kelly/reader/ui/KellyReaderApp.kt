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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

import com.kelly.reader.di.appContainer
import com.kelly.reader.ui.add.AddBooksRoute
import com.kelly.reader.ui.reader.ReaderRoute
import com.kelly.reader.ui.shelf.ShelfRoute
import com.kelly.reader.ui.splash.SplashScreen

// how long the splash stays at least, so the icon and the name get their moment
private const val SPLASH_MIN_MS = 1_500L

/**
 * The whole app below the theme. Shows the splash until it has run its course and the start
 * route is known, then fades into the shelf or the recent book. The splash survives rotation
 * and shows again only on a fresh start.
 */
@Composable
fun KellyReaderApp(modifier: Modifier = Modifier) {
    val container = appContainer()
    val appViewModel: AppViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AppViewModel(container.settingsRepository, container.bookRepository) }
        },
    )
    val startRoute by appViewModel.startRoute.collectAsStateWithLifecycle()
    var splashElapsed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!splashElapsed) {
            // count from the first drawn frame, not from composition: on a slow start the
            // system splash covers the app for a while and the timer must not run under it
            withFrameNanos { }
            delay(SPLASH_MIN_MS)
            splashElapsed = true
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val route = startRoute
        Crossfade(targetState = if (splashElapsed) route else null, label = "splash") { target ->
            if (target == null) {
                SplashScreen()
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
