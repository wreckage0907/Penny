package com.penny.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * The v2.1 component library: 48dp minimum targets, 8dp separation, pill
 * buttons, bordered cards, notice banners, tags, tracks and the labelled
 * bottom navigation — everything the artboard frames share. Every
 * assertable node carries a testTag (repo rule).
 */

/** Money text uses tabular figures (artboard "Rules" legend). */
val MoneyStyle = TextStyle(fontFeatureSettings = "tnum")

internal fun Modifier.tag(name: String?): Modifier = if (name == null) this else testTag(name)

// ── Buttons ────────────────────────────────────────────────────────────────

enum class BtnKind { PRIMARY, SECONDARY, GHOST, DANGER }

@Composable
fun PennyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.PRIMARY,
    small: Boolean = false,
    full: Boolean = true,
    leadingIcon: ImageVector? = null,
    tag: String? = null,
) {
    val (bg, fg, border) = when (kind) {
        BtnKind.PRIMARY -> Triple(PennyPalette.brand, PennyPalette.surface, Color.Transparent)
        BtnKind.SECONDARY -> Triple(PennyPalette.surface, PennyPalette.ink, PennyPalette.ink.copy(alpha = 0.14f))
        BtnKind.GHOST -> Triple(Color.Transparent, PennyPalette.ink, PennyPalette.ink.copy(alpha = 0.16f))
        BtnKind.DANGER -> Triple(PennyPalette.neg, PennyPalette.surface, Color.Transparent)
    }
    Row(
        modifier = modifier
            .tag(tag)
            .then(if (full) Modifier.fillMaxWidth() else Modifier)
            .clip(PennyShape.pill)
            .background(bg)
            .border(1.dp, border, PennyShape.pill)
            .defaultMinSize(minHeight = if (small) 48.dp else 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(7.dp))
        }
        Text(
            text,
            color = fg,
            style = TextStyle(fontSize = if (small) 13.sp else 14.sp, fontWeight = FontWeight.W800),
        )
    }
}

/** 48dp circular bordered icon target (back, close, edit, delete…). */
@Composable
fun IconBtn(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PennyPalette.ink,
    bg: Color = PennyPalette.surface,
    tag: String? = null,
) {
    Box(
        modifier = modifier
            .tag(tag)
            .size(48.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, PennyPalette.ink.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint)
    }
}

@Composable
fun PillBtn(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    goldy: Boolean = false,
    tag: String? = null,
) {
    val bg = when {
        selected -> PennyPalette.brand
        goldy -> PennyPalette.goldBg
        else -> PennyPalette.sand
    }
    val fg = when {
        selected -> PennyPalette.surface
        goldy -> PennyPalette.gold
        else -> PennyPalette.muted
    }
    val border = when {
        selected -> PennyPalette.brand
        goldy -> PennyPalette.goldRing
        else -> PennyPalette.ink.copy(alpha = 0.10f)
    }
    Text(
        text,
        color = fg,
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800),
        modifier = modifier
            .tag(tag)
            .clip(PennyShape.pill)
            .background(bg)
            .border(1.dp, border, PennyShape.pill)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Inline accent action ("Forgot password?", "See all →"). */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null) {
    Text(
        text,
        color = PennyPalette.brand,
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800),
        modifier = modifier
            .tag(tag)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
    )
}

// ── Tags & notices ─────────────────────────────────────────────────────────

enum class TagKind { PLAIN, POS, NEG, BRAND, GOLD }

@Composable
fun Tag(text: String, modifier: Modifier = Modifier, kind: TagKind = TagKind.PLAIN, tag: String? = null) {
    val (bg, fg, border) = when (kind) {
        TagKind.PLAIN -> Triple(PennyPalette.sand, PennyPalette.muted, Color.Transparent)
        TagKind.POS -> Triple(PennyPalette.posBg, PennyPalette.pos, Color.Transparent)
        TagKind.NEG -> Triple(PennyPalette.negBg, PennyPalette.neg, Color.Transparent)
        TagKind.BRAND -> Triple(PennyPalette.soft, Color(0xFF934B4B), Color.Transparent)
        TagKind.GOLD -> Triple(PennyPalette.goldBg, PennyPalette.gold, PennyPalette.goldRing)
    }
    Text(
        text,
        color = fg,
        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W800),
        modifier = modifier
            .tag(tag)
            .clip(PennyShape.pill)
            .background(bg)
            .border(1.dp, border, PennyShape.pill)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    )
}

enum class NoticeKind { PLAIN, POS, NEG, GOLD }

/** Icon + small text banner (offline, saved, confirmations, cautions). */
@Composable
fun Notice(
    text: String,
    modifier: Modifier = Modifier,
    kind: NoticeKind = NoticeKind.PLAIN,
    icon: ImageVector = Icons.Filled.Info,
    boldParts: List<String> = emptyList(),
    tag: String? = null,
) {
    val (bg, fg, border) = when (kind) {
        NoticeKind.PLAIN -> Triple(PennyPalette.sand, PennyPalette.muted, PennyPalette.ink.copy(alpha = 0.10f))
        NoticeKind.POS -> Triple(PennyPalette.posBg, PennyPalette.pos, PennyPalette.pos.copy(alpha = 0.18f))
        NoticeKind.NEG -> Triple(PennyPalette.negBg, PennyPalette.neg, PennyPalette.neg.copy(alpha = 0.20f))
        NoticeKind.GOLD -> Triple(PennyPalette.goldBg, PennyPalette.gold, PennyPalette.goldRing)
    }
    Row(
        modifier = modifier
            .tag(tag)
            .clip(PennyShape.field)
            .background(bg)
            .border(1.dp, border, PennyShape.field)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        BoldBody(text, boldParts, fg)
    }
}

/** Small multi-line body with the listed substrings bold (artboard copy). */
@Composable
fun BoldBody(text: String, boldParts: List<String>, color: Color) {
    BasicText(
        annotated(text, boldParts),
        style = PennyType.meta.copy(color = color, lineHeight = 18.sp),
    )
}

private fun annotated(text: String, boldParts: List<String>) = buildAnnotatedString {
    var rest = text
    val parts = boldParts.filter { it.isNotEmpty() }
    while (rest.isNotEmpty()) {
        val hits = parts.mapNotNull { p -> rest.indexOf(p).takeIf { it >= 0 }?.let { it to p } }
        if (hits.isEmpty()) { append(rest); break }
        val (at, part) = hits.minBy { it.first }
        append(rest.take(at))
        withStyle(SpanStyle(fontWeight = FontWeight.W800)) { append(part) }
        rest = rest.drop(at + part.length)
    }
}

// ── Cards & rows ──────────────────────────────────────────────────────────

/** Bordered 18dp-radius surface card; content is the caller's Column/Row. */
@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    soft: Boolean = false,
    tag: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .tag(tag)
            .clip(PennyShape.card)
            .background(if (soft) PennyPalette.soft else PennyPalette.surface)
            .border(
                1.dp,
                if (soft) PennyPalette.brand.copy(alpha = 0.16f) else PennyPalette.ink.copy(alpha = 0.09f),
                PennyShape.card,
            )
            .padding(16.dp),
    ) { content() }
}

@Composable
fun SectionHead(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.padding(top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = PennyType.sectionSmall)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = PennyPalette.muted) {
    Text(text.uppercase(), color = color, style = PennyType.eyebrow, modifier = modifier)
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, boldParts: List<String> = emptyList()) {
    BoldBody(text, boldParts, PennyPalette.muted)
}

/** Progress track: sand rail + tinted fill. */
@Composable
fun Track(
    progress: Double,
    modifier: Modifier = Modifier,
    color: Color = PennyPalette.brand,
    height: Int = 6,
    tag: String? = null,
) {
    Box(
        modifier
            .tag(tag)
            .fillMaxWidth()
            .height(height.dp)
            .clip(PennyShape.pill)
            .background(PennyPalette.sand),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0.0, 1.0).toFloat())
                .height(height.dp)
                .clip(PennyShape.pill)
                .background(color),
        )
    }
}

/** Loading placeholder (S04); gentle pulse, never looks like real data. */
@Composable
fun Skeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PennyPalette.sand.copy(alpha = alpha)),
    )
}

@Composable
fun TextDivider(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(PennyPalette.ink.copy(alpha = 0.10f)))
        Text(
            text.uppercase(),
            color = PennyPalette.muted,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W800, letterSpacing = 0.8.sp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(PennyPalette.ink.copy(alpha = 0.10f)))
    }
}

// ── Rows ───────────────────────────────────────────────────────────────────

/** Category/transaction summary row (Home, Budget, offline lists). */
@Composable
fun SpendRow(
    name: String,
    meta: String,
    amount: String,
    dot: Color,
    modifier: Modifier = Modifier,
    miniTrack: Double? = null,
    onClick: (() -> Unit)? = null,
    tag: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .tag(tag)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(Modifier.size(11.dp).clip(RoundedCornerShape(4.dp)).background(dot))
        Column(Modifier.weight(1f)) {
            Text(name, style = PennyType.secondary, modifier = Modifier.tag(tag?.let { "$it-name" }))
            Text(meta, color = PennyPalette.muted, style = PennyType.meta, modifier = Modifier.padding(top = 3.dp).tag(tag?.let { "$it-meta" }))
            if (miniTrack != null) {
                Track(miniTrack, Modifier.padding(top = 7.dp), color = dot, height = 5)
            }
        }
        Text(amount, color = PennyPalette.neg, style = PennyType.amountS.merge(MoneyStyle), modifier = Modifier.tag(tag?.let { "$it-amount" }))
        trailing?.invoke()
    }
}

/** Settings list row (Profile frames): key grows, value right-aligned. */
@Composable
fun ListRow(
    key: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    value: String? = null,
    tag: String? = null,
) {
    Row(
        modifier = modifier
            .tag(tag)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(key, style = PennyType.secondary, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, color = PennyPalette.muted, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W700))
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = PennyPalette.muted,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Grouped settings list container (artboard .list). */
@Composable
fun ListGroup(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PennyPalette.surface)
            .border(1.dp, PennyPalette.ink.copy(alpha = 0.09f), RoundedCornerShape(16.dp)),
        content = content,
    )
}

/** Hairline between list rows. */
@Composable
fun ListDivider() {
    Box(Modifier.padding(horizontal = 14.dp).height(1.dp).background(PennyPalette.ink.copy(alpha = 0.08f)))
}

// ── Fields ─────────────────────────────────────────────────────────────────

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    readOnly: Boolean = false,
    keyboardDecimal: Boolean = false,
    password: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    tag: String? = null,
) {
    Column(modifier.tag(tag).padding(top = 14.dp)) {
        Text(
            label,
            color = PennyPalette.ink,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800),
            modifier = Modifier.padding(start = 2.dp, bottom = 7.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .clip(PennyShape.field)
                .background(PennyPalette.sand)
                .border(
                    1.dp,
                    if (error != null) PennyPalette.neg else PennyPalette.ink.copy(alpha = 0.10f),
                    PennyShape.field,
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (prefix != null) Text(prefix, style = PennyType.secondaryPlain)
            androidx.compose.material3.OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = true,
                placeholder = if (placeholder != null) {
                    { Text(placeholder, color = PennyPalette.muted, style = PennyType.secondaryPlain) }
                } else null,
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W600, color = PennyPalette.ink),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = if (keyboardDecimal) KeyboardType.Decimal else KeyboardType.Text,
                ),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.weight(1f).tag(tag?.let { "$it-input" }),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = PennyPalette.brand,
                ),
            )
            trailing?.invoke()
        }
        if (error != null) {
            Text(
                error,
                color = PennyPalette.neg,
                style = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
                modifier = Modifier.padding(start = 2.dp, top = 6.dp),
            )
        } else if (hint != null) {
            Hint(hint, Modifier.padding(start = 2.dp, top = 7.dp))
        }
    }
}

// ── Avatar ────────────────────────────────────────────────────────────────

@Composable
fun Avatar(
    initials: String,
    imageUrl: String?,
    size: Int = 48,
    tag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickable = Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .tag(tag)
                .size(size.dp)
                .clip(CircleShape)
                .then(clickable),
        )
    } else {
        Box(
            modifier = Modifier
                .tag(tag)
                .size(size.dp)
                .clip(CircleShape)
                .background(PennyPalette.blush)
                .then(clickable),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = PennyPalette.onBlush, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800))
        }
    }
}

// ── Bottom navigation + dock ──────────────────────────────────────────────

enum class PennyTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    BUDGET("Budget", Icons.Filled.AccountBalanceWallet),
    LEARN("Learn", Icons.Filled.MenuBook),
    PROFILE("Profile", Icons.Filled.Person),
}

/** 76dp labelled bar; the active tab's icon sits in a blush ring. */
@Composable
fun BottomNavBar(current: PennyTab, onSelect: (PennyTab) -> Unit, tag: String? = "bottom-nav") {
    Surface(
        color = PennyPalette.bg.copy(alpha = 0.96f),
        modifier = Modifier.tag(tag).fillMaxWidth().height(76.dp),
    ) {
        Row {
            PennyTab.entries.forEach { tab ->
                val selected = tab == current
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .tag("nav-${tab.label.lowercase()}")
                        .clickable { onSelect(tab) }
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 30.dp)
                            .clip(PennyShape.pill)
                            .background(if (selected) PennyPalette.blush else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) PennyPalette.ink else PennyPalette.muted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        tab.label,
                        color = if (selected) PennyPalette.ink else PennyPalette.muted,
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W700),
                    )
                }
            }
        }
    }
}

/** Thumb-zone dock above the bar (frame 04: Log Expense + Plan shortcut). */
@Composable
fun Dock(
    vararg actions: Pair<String, () -> Unit>,
    primaryIcon: ImageVector? = Icons.Filled.Add,
    tag: String? = "dock",
) {
    Surface(
        color = PennyPalette.bg.copy(alpha = 0.96f),
        modifier = Modifier.tag(tag).fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            actions.forEachIndexed { i, (label, onClick) ->
                PennyButton(
                    label,
                    onClick = onClick,
                    kind = if (i == 0) BtnKind.PRIMARY else BtnKind.SECONDARY,
                    small = true,
                    leadingIcon = if (i == 0) primaryIcon else null,
                    modifier = Modifier.weight(1f),
                    tag = "dock-$i",
                )
            }
        }
    }
}

// ── Overlays ───────────────────────────────────────────────────────────────

/** Artboard .modal: dimmed scrim + centred 24dp-radius card. */
@Composable
fun ConfirmModal(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    headerIcon: ImageVector? = null,
    headerTint: Color = PennyPalette.neg,
    headerBg: Color = PennyPalette.negBg,
    tag: String? = "modal",
    body: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
) {
    // The scrim is clickable (tap-outside dismisses) but the card sits on
    // top as a SIBLING, so the card's own fields/buttons keep their
    // semantics instead of being merged into the scrim.
    Box(
        Modifier
            .tag(tag)
            .fillMaxWidth()
            .height(844.dp),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(PennyPalette.ink.copy(alpha = 0.48f))
                .clickable(onClick = onDismiss),
        )
        Box(Modifier.fillMaxWidth().height(844.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = modifier
                    .padding(20.dp)
                    .clip(PennyShape.modal)
                    .background(PennyPalette.surface)
                    .padding(20.dp),
            ) {
                if (headerIcon != null) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(headerBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(headerIcon, contentDescription = null, tint = headerTint)
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Text(title, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.W800))
                body?.invoke(this)
            }
        }
    }
}

/**
 * Artboard bottom sheet: a grab handle, scrim and rounded top corners.
 * Rendered IN the screen (not a separate ModalBottomSheet window) so every
 * target shares one semantics tree — stable on slower devices and in CI.
 */
@Composable
fun PennySheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().tag(tag)) {
        Box(
            Modifier
                .matchParentSize()
                .background(PennyPalette.ink.copy(alpha = 0.40f))
                .clickable(onClick = onDismiss),
        )
        Column(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(PennyShape.sheet)
                .background(PennyPalette.surface)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(width = 40.dp, height = 5.dp)
                    .clip(PennyShape.pill)
                    .background(PennyPalette.ink.copy(alpha = 0.18f)),
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Artboard .toast: ink bar near the bottom with an optional undo action. */
@Composable
fun ToastBar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    tag: String? = null,
) {
    Row(
        modifier = modifier
            .tag(tag)
            .clip(PennyShape.field)
            .background(PennyPalette.ink)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(message, color = PennyPalette.bg, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W600), modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                color = Color(0xFFF1C6C6),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800),
                modifier = Modifier
                    .tag(tag?.let { "$it-action" })
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(4.dp),
            )
        }
    }
}

/** Back arrow + title + optional side text (frames 02/10/11/15 navbar). */
@Composable
fun NavBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    side: String? = null,
    sideColor: Color = PennyPalette.muted,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    tag: String? = null,
) {
    Row(
        modifier.tag(tag).padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) {
            IconBtn(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, tag = "back")
        }
        leading?.invoke()
        Text(title, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800), modifier = Modifier.weight(1f))
        if (side != null) {
            Text(side, color = sideColor, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W800))
        }
        trailing?.invoke()
    }
}

/**
 * The v1 flat-coloured toast strip (profileFlows Toast kinds) — retained for
 * the stock practice screen, which still reports trades this way.
 */
@Composable
fun ToastHost(toasts: MutableList<Pair<com.penny.app.core.ProfileFlows.Toast, String>>, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        toasts.forEachIndexed { i, (kind, text) ->
            val color = when (kind) {
                com.penny.app.core.ProfileFlows.Toast.SUCCESS -> Color(0xFF4CAF50)
                com.penny.app.core.ProfileFlows.Toast.ERROR -> Color(0xFFF44336)
                com.penny.app.core.ProfileFlows.Toast.INFO -> Color(0xFF2196F3)
                com.penny.app.core.ProfileFlows.Toast.WARNING -> Color(0xFFFF9800)
            }
            Text(
                text,
                Modifier
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("toast-${kind.name.lowercase()}"),
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    LaunchedEffect(toasts.size) {
        if (toasts.isNotEmpty()) {
            kotlinx.coroutines.delay(4_000)
            toasts.clear()
        }
    }
}
