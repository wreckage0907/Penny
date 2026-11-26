package com.penny.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.core.PennyColors

/**
 * v2.1 "Warm Terracotta" design system (`.lavish/penny-v21-artboard.html`
 * :root). The four existing brand colours (Ink, Linen, Blush, Terracotta in
 * core/AppRoutes.kt) keep their roles; the washes (sand, soft, sage, gold,
 * neg) and the five category colours are the v2 additions the artboard
 * locked in. Single light theme, like the Flutter app.
 */
object PennyPalette {
    val ink = Color(0xFF35333A)
    val bg = Color(PennyColors.BACKGROUND) // Linen #F9F5F2
    val brand = Color(PennyColors.BUTTON) // Terracotta #A35555
    val blush = Color(PennyColors.CARD) // #E4C7C3
    val surface = Color(0xFFFFFDFB)
    val sand = Color(0xFFF2EBE6)
    val soft = Color(0xFFF6EAE8)
    val muted = Color(0xFF625E68)
    val pos = Color(0xFF2B6E4C)
    val posBg = Color(0xFFE3F1EA)
    val neg = Color(0xFFB03A3A)
    val negBg = Color(0xFFF9E6E6)
    val gold = Color(0xFF7C5824)
    val goldBg = Color(0xFFFBF1E4)
    val goldRing = Color(0xFFE6C79C)
    val onBlush = Color(0xFF4A3A3F)
    val heroInk = Color(0xFF35333A)
    val heroShade = Color(0xFF46353B)

    /** Artboard c1..c5 by category order (Food, Shopping, Rent, Transport, Entertainment). */
    val c1 = Color(0xFFA35555)
    val c2 = Color(0xFFC87D55)
    val c3 = Color(0xFF5B7065)
    val c4 = Color(0xFF4A6B82)
    val c5 = Color(0xFF7D5BA6)
}

/**
 * The artboard's locked type scale: "16sp reading body · 14sp secondary ·
 * 12sp metadata · tabular money". Display sizes for hero/phone titles.
 */
object PennyType {
    val hero = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.W800, letterSpacing = (-0.75).sp)
    val phoneTitle = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.W800, letterSpacing = (-0.75).sp, lineHeight = 30.sp)
    val sheetTitle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.W800)
    val section = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.W800, letterSpacing = (-0.25).sp)
    val sectionSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W800)
    val body = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W400, lineHeight = 26.sp)
    val secondary = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W700)
    val secondaryPlain = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W400, lineHeight = 22.sp)
    val meta = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W400, lineHeight = 18.sp)
    val metaBold = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W800)
    val eyebrow = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.W800, letterSpacing = 1.1.sp)
    val amountL = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.W800)
    val amountM = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800)
    val amountS = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W800)
}

@Composable
fun PennyTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = PennyPalette.brand,
        onPrimary = PennyPalette.surface,
        background = PennyPalette.bg,
        onBackground = PennyPalette.ink,
        surface = PennyPalette.surface,
        onSurface = PennyPalette.ink,
        secondaryContainer = PennyPalette.sand,
        onSecondaryContainer = PennyPalette.ink,
        error = PennyPalette.neg,
        outlineVariant = Color(0x17FFFFFF).copy(alpha = 0.0f), // unused; borders come from PennyPalette
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            titleLarge = PennyType.phoneTitle,
            titleMedium = PennyType.sheetTitle,
            headlineSmall = PennyType.section,
            bodyLarge = PennyType.body,
            bodyMedium = PennyType.secondaryPlain,
            bodySmall = PennyType.meta,
            labelLarge = PennyType.secondary,
            labelSmall = PennyType.eyebrow,
        ),
        content = content,
    )
}

/** Shared corner radii (artboard: cards 18, inputs/notices 14, modals 24, sheets 26). */
object PennyShape {
    val card = RoundedCornerShape(18.dp)
    val field = RoundedCornerShape(14.dp)
    val modal = RoundedCornerShape(24.dp)
    val sheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    val pill = RoundedCornerShape(999.dp)
    val small = RoundedCornerShape(13.dp)
}
