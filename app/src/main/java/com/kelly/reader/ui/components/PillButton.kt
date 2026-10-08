/*
 * The design's buttons: a filled pill with an optional leading icon, and a plain text button.
 */
package com.kelly.reader.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.kelly.reader.ui.theme.AppTheme

/**
 * A filled pill button, 44dp tall by default, with [icon] before the [label] when given.
 */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(
            start = if (icon == null) 18.dp else 14.dp,
            end = 18.dp,
        ),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * A text-only button in the accent text color, 44dp tall.
 */
@Composable
fun AccentTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.accentText),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
