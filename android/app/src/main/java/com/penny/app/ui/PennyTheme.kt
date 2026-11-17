package com.penny.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.penny.app.core.PennyColors

/** AppColours (consts/app_colours.dart) as a Material 3 scheme. */
object PennyPalette {
    val text = Color(PennyColors.TEXT)
    val background = Color(PennyColors.BACKGROUND)
    val card = Color(PennyColors.CARD)
    val button = Color(PennyColors.BUTTON)
}

@Composable
fun PennyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PennyPalette.button,
            onPrimary = PennyPalette.background,
            background = PennyPalette.background,
            onBackground = PennyPalette.text,
            surface = PennyPalette.background,
            onSurface = PennyPalette.text,
            secondaryContainer = PennyPalette.card,
        ),
        content = content,
    )
}
