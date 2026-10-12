/*
 * The app's icons, built from the stroke paths in the design prototype. All are 24x24 line
 * icons; the stroke color is black and replaced by the Icon tint.
 */
package com.kelly.reader.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Builds a 24dp line icon named [name] from SVG path strings, stroked [strokeWidth] wide.
 */
private fun lineIcon(name: String, strokeWidth: Float, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { data ->
            addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/**
 * A circle path centered at ([cx], [cy]) with radius [r], as two arcs.
 */
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx + r} ${cy}a$r $r 0 1 1 ${-2 * r} 0a$r $r 0 1 1 ${2 * r} 0"

object AppIcons {
    val PLUS = lineIcon("plus", 2.2f, "M12 5v14M5 12h14")
    val CHECK = lineIcon("check", 3f, "M5 12.5l4.5 4.5L19 7.5")
    val TRASH = lineIcon("trash", 2f, "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val BACK = lineIcon("back", 2f, "M15 5l-7 7 7 7")
    val CLOSE = lineIcon("close", 2f, "M6 6l12 12M18 6L6 18")
    val BOOK = lineIcon(
        "book", 1.8f,
        "M4 19V5a2 2 0 0 1 2-2h13v14H6a2 2 0 0 0-2 2z",
        "M4 19a2 2 0 0 0 2 2h13v-4",
    )
    val GRID = lineIcon(
        "grid", 2f,
        "M5 4h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
        "M15 4h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
        "M5 14h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-4a1 1 0 0 1 1-1z",
        "M15 14h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1v-4a1 1 0 0 1 1-1z",
    )
    val WIFI = lineIcon(
        "wifi", 2f,
        "M2.5 9a14 14 0 0 1 19 0",
        "M5.5 12.5a9.5 9.5 0 0 1 13 0",
        "M8.5 16a5 5 0 0 1 7 0",
        circle(12f, 19.5f, 1f),
    )
    val COPY = lineIcon(
        "copy", 2f,
        "M11 9h7a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z",
        "M5 15V5a1 1 0 0 1 1-1h9",
    )
    val FILE = lineIcon("file", 1.8f, "M7 3h7l5 5v13H7z", "M14 3v5h5")
    val ALERT = lineIcon("alert", 3f, "M12 6v8", "M12 18.5v.5")
    val SLIDERS = lineIcon(
        "sliders", 2f,
        "M4 7h10M18 7h2M4 17h4M12 17h8",
        circle(16f, 7f, 2f),
        circle(10f, 17f, 2f),
    )
}
