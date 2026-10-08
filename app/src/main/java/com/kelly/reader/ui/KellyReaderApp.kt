/*
 * Root of the Compose UI: the navigation host and the three screens.
 */
package com.kelly.reader.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.kelly.reader.ui.add.AddBooksRoute
import com.kelly.reader.ui.reader.ReaderRoute
import com.kelly.reader.ui.shelf.ShelfRoute

/**
 * The whole app below the theme. Starts on the shelf; the launch rule for a recent book comes
 * with the app ViewModel.
 */
@Composable
fun KellyReaderApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AppNavHost(navController = navController, startRoute = Routes.SHELF)
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
                onBackToShelf = { navController.popBackStack() },
            )
        }
    }
}
