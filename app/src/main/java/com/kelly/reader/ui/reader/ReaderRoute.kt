package com.kelly.reader.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The reader screen entry. A placeholder until the reader lands. The system back button leaves
 * for the shelf, the same as the drawer's button.
 */
@Composable
fun ReaderRoute(
    bookId: String,
    onBackToShelf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackToShelf)
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Reader: $bookId")
        Button(onClick = onBackToShelf) { Text(text = "Back to bookshelf") }
    }
}
