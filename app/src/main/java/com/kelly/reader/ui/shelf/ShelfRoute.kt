package com.kelly.reader.ui.shelf

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The bookshelf screen entry. A placeholder until the shelf screen lands.
 */
@Composable
fun ShelfRoute(
    onOpenBook: (String) -> Unit,
    onAddBooks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Bookshelf")
        Button(onClick = onAddBooks) { Text(text = "Add") }
        Button(onClick = { onOpenBook("placeholder") }) { Text(text = "Open a book") }
    }
}
