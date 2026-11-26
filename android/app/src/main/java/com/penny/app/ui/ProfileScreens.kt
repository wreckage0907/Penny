@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.penny.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.core.AuthFlows
import com.penny.app.core.DashboardFlows
import com.penny.app.core.ExpenseAggregation
import com.penny.app.core.ExpenseDashboardText
import com.penny.app.core.HomeLogic
import com.penny.app.core.LearnState
import com.penny.app.core.PlanMath
import com.penny.app.core.ProfileExtras
import com.penny.app.core.ProfileFlows
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate

/**
 * v2.1 Profile & settings (artboard frames 14–15 + state frames S02/S05):
 * a saved-state banner after edits, achievements from the learning journey,
 * honest preference rows, a delete-account modal with the exact data impact
 * and reauthentication, an unsaved-changes guard on back, and CSV export.
 *
 * Superseded v1 quirks (ledger): the four-toast save celebration and the
 * "photo deletion fails" bug are gone — v2.1 saves silently and confirms
 * with one banner, and photo removal is staged and reversible.
 */

// ── 14 · Profile & settings ───────────────────────────────────────────────

@Composable
fun ProfileScreen(
    services: PennyServices,
    navigate: (String) -> Unit,
    toLogin: () -> Unit,
    savedBanner: Boolean = false,
    tab: PennyTab = PennyTab.PROFILE,
) {
    val scope = rememberCoroutineScope()
    var identity by remember { mutableStateOf<Pair<String?, String?>>(null to null) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var learn by remember { mutableStateOf(services.learn.state.value) }
    LaunchedEffect(services.learn.state) { services.learn.state.collect { learn = it } }
    var txCount by remember { mutableStateOf(0) }
    var passwordOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var exportOpen by remember { mutableStateOf(false) }
    var csv by remember { mutableStateOf("") }
    var banner by remember { mutableStateOf(savedBanner) }

    LaunchedEffect(Unit) {
        val claims = services.auth.customClaims()
        identity = HomeLogic.identity(claims, services.prefs.username(), services.prefs.fullName())
        avatarUrl = try {
            services.profileImages.imageUrl(identity.first ?: "")
        } catch (e: Exception) {
            null
        }
        val user = identity.first ?: ""
        val today = LocalDate.now()
        val month = ExpenseDashboardText.initialMonth(today.monthValue)
        val expenses = try {
            services.expenses(user).getExpenses(today.year, month)
        } catch (e: Exception) {
            kotlinx.serialization.json.JsonObject(emptyMap())
        }
        txCount = PlanMath.monthCategories(expenses, today.year, month).values.sumOf { it.second }
        csv = ProfileExtras.csv(csvRows(services, user, today.year, month))
    }

    val (username, fullName) = identity
    val initials = (fullName ?: "?").split(' ').filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }
    val monthLabel = PlanMath.monthLabel(LocalDate.now().year, ExpenseDashboardText.initialMonth(LocalDate.now().monthValue))
    var email by remember { mutableStateOf("") }
    LaunchedEffect(username) {
        email = AuthFlows.emailAndFullName(
            try { services.users.getUser(username ?: "") } catch (e: Exception) { null },
        ).first ?: ""
    }

    Column(Modifier.fillMaxSize().tag("profile-page")) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 5.dp, bottom = 24.dp),
        ) {
            Row(Modifier.padding(top = 5.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Profile", style = PennyType.phoneTitle, modifier = Modifier.weight(1f).tag("profile-title"))
                Tag("INR · ₹", tag = "profile-currency")
            }
            if (banner) {
                Notice(
                    "Profile saved. Your changes are up to date.",
                    Modifier, NoticeKind.POS, icon = Icons.Filled.Check,
                    boldParts = listOf("Profile saved."),
                    tag = "profile-saved-banner",
                )
            }
            CardBox(Modifier.padding(top = 14.dp), tag = "profile-card") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Avatar(initials, avatarUrl, size = 64, tag = "profile-avatar")
                        Column(Modifier.weight(1f)) {
                            Text(fullName ?: "—", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800), modifier = Modifier.tag("profile-fullname"))
                            Text(
                                "@${username ?: ""} · $email",
                                color = PennyPalette.muted,
                                style = PennyType.meta,
                                modifier = Modifier.padding(top = 3.dp).tag("profile-id"),
                            )
                        }
                        PennyButton("Edit", { navigate("editprofile") }, full = false, small = true, kind = BtnKind.SECONDARY, tag = "profile-edit")
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag("🔥 ${learn.streakDays}-day streak", Modifier.weight(1f), kind = TagKind.GOLD, tag = "profile-streak")
                        Tag("◈ ${learn.coins} coins", Modifier.weight(1f), kind = TagKind.GOLD, tag = "profile-coins")
                        Tag("★ ${learn.badges.size} badges", Modifier.weight(1f), kind = TagKind.GOLD, tag = "profile-badges")
                    }
                }
            }
            SectionLabel("Preferences")
            ListGroup {
                ListRow("Currency", value = "INR (₹) · display only", tag = "pref-currency")
                ListDivider()
                ListRow("Budget Rule", value = "50 / 30 / 20", tag = "pref-budget-rule")
                ListDivider()
                ListRow("Audio TTS Speed", value = "1.0×", tag = "pref-tts")
            }
            SectionLabel("Account & Security")
            ListGroup {
                ListRow("Personal Info", onClick = { navigate("editprofile") }, tag = "security-info")
                ListDivider()
                ListRow("Change Password", onClick = { passwordOpen = true }, tag = "profile-change-password")
                ListDivider()
                ListRow("Export CSV", onClick = { exportOpen = true }, value = "$txCount rows · ${monthLabel.take(3)}", tag = "profile-export")
            }
            SectionLabel("Tools")
            ListGroup {
                ListRow("Market Practice", onClick = { navigate("stockprofile") }, value = "local only", tag = "tools-stocks")
            }
            PennyButton(
                "Sign Out",
                {
                    scope.launch {
                        AuthFlows.signOut(services.auth, services.prefs)
                        toLogin()
                    }
                },
                Modifier.padding(top = 18.dp), kind = BtnKind.GHOST, tag = "profile-sign-out",
            )
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .clickable { deleteOpen = true }
                    .fillMaxWidth()
                    .tag("profile-delete"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Delete Account",
                    color = PennyPalette.neg,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800),
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
        BottomNavBar(tab, { t -> navigate(tabRoute(t)) })
    }

    if (passwordOpen) {
        ChangePasswordSheet(services, onDismiss = { passwordOpen = false })
    }
    if (exportOpen) {
        ConfirmModal("Export CSV — $monthLabel", onDismiss = { exportOpen = false }, tag = "export-modal") {
            Text(
                "$txCount rows, one per expense.",
                color = PennyPalette.muted,
                style = PennyType.secondaryPlain,
                modifier = Modifier.padding(top = 8.dp),
            )
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PennyPalette.sand)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(10.dp)
                    .tag("export-csv"),
            ) {
                Text(csv, style = TextStyle(fontSize = 11.sp, lineHeight = 16.sp))
            }
            PennyButton("Done", { exportOpen = false }, Modifier.padding(top = 12.dp), tag = "export-close")
        }
    }
    if (deleteOpen) {
        DeleteAccountModal(
            services = services,
            username = username ?: "",
            txCount = txCount,
            monthLabel = monthLabel,
            learn = learn,
            hasPhoto = avatarUrl != null,
            onDismiss = { deleteOpen = false },
            onDeleted = toLogin,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = PennyPalette.muted,
        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W800, letterSpacing = 1.sp),
        modifier = Modifier.padding(top = 20.dp, start = 3.dp, bottom = 8.dp),
    )
}

private suspend fun csvRows(
    services: PennyServices,
    user: String,
    year: Int,
    month: String,
): List<ProfileExtras.CsvExpense> {
    val expenses = try {
        services.expenses(user).getExpenses(year, month)
    } catch (e: Exception) {
        JsonObject(emptyMap())
    }
    val categories = ExpenseAggregation.categoriesFor(expenses, year, month)
    return categories.flatMap { (category, list) ->
        ((list as? JsonArray) ?: emptyList()).mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val name = (obj["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            val amount = (obj["amount"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return@mapNotNull null
            val detail = services.meta.detail(user, year, month, category, name)
            ProfileExtras.CsvExpense(
                date = "${detail.day ?: ""} $month",
                category = category,
                merchant = name,
                payment = detail.payment,
                amount = amount,
            )
        }
    }
}

@Composable
private fun ChangePasswordSheet(services: PennyServices, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = PennyShape.sheet,
        containerColor = PennyPalette.surface,
    ) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 16.dp).tag("pw-sheet")) {
            Text("Change Password", style = PennyType.sheetTitle)
            Field("Current password", current, { current = it; error = null }, password = true, tag = "pw-current")
            Field("New password", new, { new = it; error = null }, password = true, hint = "Use at least 6 characters.", tag = "pw-new")
            Field("Confirm new password", confirm, { confirm = it; error = null }, password = true, tag = "pw-confirm")
            if (error != null) {
                Notice(error!!, Modifier.padding(top = 10.dp), NoticeKind.NEG, tag = "pw-error")
            }
            if (done) {
                Notice("Password changed successfully.", Modifier.padding(top = 10.dp), NoticeKind.POS, icon = Icons.Filled.Check, tag = "pw-done")
            }
            PennyButton(
                "Change Password",
                {
                    scope.launch {
                        val outcome = ProfileFlows.changePassword(services.auth, current, new, confirm)
                        when (outcome) {
                            is ProfileFlows.Outcome.Done -> done = true
                            is ProfileFlows.Outcome.Failed -> error = outcome.message
                            ProfileFlows.Outcome.Silent -> error = "Could not change the password right now."
                        }
                    }
                },
                Modifier.padding(top = 12.dp),
                tag = "pw-change",
            )
        }
    }
}

// ── S02 · Delete account ───────────────────────────────────────────────────

@Composable
private fun DeleteAccountModal(
    services: PennyServices,
    username: String,
    txCount: Int,
    monthLabel: String,
    learn: LearnState,
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ConfirmModal(
        "Permanently delete your account?",
        onDismiss = onDismiss,
        headerIcon = Icons.Filled.Delete,
        tag = "delete-account-modal",
    ) {
        Text(
            "This cannot be undone. The following will be permanently deleted:",
            color = PennyPalette.muted,
            style = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
            modifier = Modifier.padding(top = 10.dp),
        )
        ProfileExtras.deleteImpact(
            transactionCount = txCount,
            monthLabel = monthLabel.take(3),
            coins = learn.coins,
            streakDays = learn.streakDays,
            badgeCount = learn.badges.size,
            unitCount = learn.completed.map { it.unit }.distinct().size,
            hasPhoto = hasPhoto,
            aiThreadCount = 0,
        ).forEach { line ->
            Text("• $line", color = PennyPalette.muted, style = TextStyle(fontSize = 13.sp, lineHeight = 20.sp))
        }
        Field(
            "Confirm your password", password, { password = it; error = null },
            password = true, placeholder = "Required to continue", tag = "delete-password",
        )
        if (error != null) {
            Notice(error!!, Modifier.padding(top = 10.dp), NoticeKind.NEG, tag = "delete-error")
        }
        PennyButton(
            if (busy) "Deleting…" else "Permanently Delete Account",
            {
                if (busy) return@PennyButton
                busy = true
                scope.launch {
                    val email = (services.auth.state.value as? com.penny.app.core.AuthState.SignedIn)?.user?.email ?: ""
                    if (!services.auth.reauthenticate(email, password)) {
                        busy = false
                        error = "That password doesn't match. Try again."
                        return@launch
                    }
                    try {
                        ProfileFlows.deleteAccount(services.auth, services.users, services.prefs, username)
                        onDeleted()
                    } catch (e: Exception) {
                        busy = false
                        error = "Could not delete the account right now. Nothing was deleted."
                    }                }
            },
            Modifier.padding(top = 12.dp), kind = BtnKind.DANGER, tag = "delete-account-confirm",
        )
        PennyButton("Keep My Account", onDismiss, Modifier.padding(top = 10.dp), kind = BtnKind.GHOST, tag = "delete-account-cancel")
    }
}

// ── 15 · Edit profile (dirty, staged photo, S05 back guard) ───────────────

@Composable
fun EditProfileScreen(services: PennyServices, onBack: () -> Unit, saved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf<String?>(null) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    // The SAVED doc, captured once after prefill — the dirty check compares
    // against this, never against the live fields (S05).
    var saved by remember { mutableStateOf<ProfileExtras.ProfileDraft?>(null) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var photoStage by remember { mutableStateOf<ProfileExtras.PhotoStage>(ProfileExtras.PhotoStage.Unchanged) }
    // Undo Remove goes back to what was staged before the removal (frame 15).
    var stageBeforeRemoval by remember { mutableStateOf<ProfileExtras.PhotoStage?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val claims = services.auth.customClaims()
        val user = DashboardFlows.username(claims, services.prefs.username()) ?: ""
        username = user
        val fields = ProfileFlows.fields(try { services.users.getUser(user) } catch (e: Exception) { null })
        fullName = "${fields.firstName} ${fields.lastName}".trim()
        email = fields.email
        phone = fields.phone
        avatarUrl = try { services.profileImages.imageUrl(user) } catch (e: Exception) { null }
        saved = ProfileExtras.ProfileDraft(fullName, email, phone, ProfileExtras.PhotoStage.Unchanged)
    }

    val dirty = saved?.let { ProfileExtras.isDirty(it, ProfileExtras.ProfileDraft(fullName, email, phone, photoStage)) }
        ?: false

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 5.dp, bottom = 24.dp)
            .tag("edit-page"),
    ) {
        NavBar(
            "Edit Profile",
            onBack = { if (dirty) confirmDiscard = true else onBack() },
            side = if (dirty) "Unsaved changes" else null,
            sideColor = if (dirty) PennyPalette.gold else PennyPalette.muted,
            tag = "edit-nav",
        )
        Column(Modifier.padding(top = 4.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val showAvatar = avatarUrl != null && photoStage != ProfileExtras.PhotoStage.Removed
            val staged = photoStage is ProfileExtras.PhotoStage.Replaced
            Box(Modifier.alpha(if (photoStage == ProfileExtras.PhotoStage.Removed) 0.45f else 1f)) {                Avatar(
                    initials = (fullName.ifBlank { "?" }).split(' ').filter { it.isNotBlank() }
                        .take(2).joinToString("") { it.first().uppercase() },
                    imageUrl = if (staged) (photoStage as ProfileExtras.PhotoStage.Replaced).url else if (showAvatar) avatarUrl else null,
                    size = 84,
                    tag = if (staged || showAvatar) "edit-avatar-image" else "edit-avatar-default",
                )
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PennyButton(
                    "Change Photo",
                    {
                        scope.launch {
                            val picked = services.pickImage?.invoke()
                            if (picked != null) {
                                photoStage = ProfileExtras.PhotoStage.Replaced(picked)
                            }
                        }
                    },
                    small = true, full = false, kind = BtnKind.SECONDARY, tag = "edit-picture",
                )
                when (photoStage) {
                    is ProfileExtras.PhotoStage.Removed -> PennyButton(
                        "Undo Remove", {
                            photoStage = stageBeforeRemoval ?: ProfileExtras.PhotoStage.Unchanged
                            stageBeforeRemoval = null
                        },
                        small = true, full = false, kind = BtnKind.GHOST, tag = "edit-undo-remove",
                    )
                    else -> PennyButton(
                        "Remove Photo", {
                            stageBeforeRemoval = photoStage
                            photoStage = ProfileExtras.PhotoStage.Removed
                        },
                        small = true, full = false, kind = BtnKind.GHOST, tag = "edit-remove-picture",
                    )
                }
            }
        }
        Notice(
            "Photo will be removed only when you save. Undo Remove keeps the current photo.",
            Modifier,
            NoticeKind.GOLD,
            boldParts = listOf("Undo Remove"),
            tag = "edit-photo-notice",
        )
        Field("Full name", fullName, { fullName = it; nameError = null }, error = nameError, tag = "edit-name")
        Field(
            "Username", "@${username ?: ""}", {}, readOnly = true,
            trailing = {
                androidx.compose.material3.Icon(
                    Icons.Filled.Lock, contentDescription = null,
                    tint = PennyPalette.muted, modifier = Modifier.padding(end = 8.dp),
                )
            },
            hint = "Chosen during sign-up and locked to protect your public identity.",
            tag = "edit-username",
        )
        Field(
            "Email", email, { email = it; emailError = null }, error = emailError,
            hint = "Changing email requires verification before it takes effect.",
            tag = "edit-email",
        )
        Field("Phone", phone, { phone = it }, hint = "Changing phone requires a new OTP.", tag = "edit-phone")
        if (saving) {
            Notice("Saving…", Modifier.padding(top = 12.dp), tag = "edit-saving")
        }
        PennyButton(
            if (dirty) "Save Changes" else "Saved",
            {
                val draftForCheck = ProfileExtras.ProfileDraft(fullName, email, phone, photoStage)
                when (ProfileExtras.validate(draftForCheck)) {
                    ProfileExtras.SaveIssue.NameRequired -> nameError = "Your name is required."
                    ProfileExtras.SaveIssue.EmailRequired -> emailError = "Your email is required."
                    ProfileExtras.SaveIssue.EmailInvalid -> emailError = "That email doesn't look valid."
                    null -> if (dirty && !saving) {
                        saving = true
                        scope.launch {
                            val user = username ?: ""
                            val first = fullName.trim().substringBefore(' ')
                            val last = fullName.trim().substringAfter(' ', "")
                            services.users.updateUser(user, first, last, email.trim(), phone.trim())
                            val uid = (services.auth.state.value as? com.penny.app.core.AuthState.SignedIn)?.user?.uid
                            if (uid != null) services.users.updateCustomClaims(uid, user, fullName.trim())
                            services.prefs.saveFullName(fullName.trim())
                            when (val stage = photoStage) {
                                is ProfileExtras.PhotoStage.Replaced -> services.profileImages.upload(user, stage.url)
                                ProfileExtras.PhotoStage.Removed -> services.profileImages.delete(user)
                                ProfileExtras.PhotoStage.Unchanged -> {}
                            }
                            saving = false
                            saved()
                        }
                    }
                }
            },
            Modifier.padding(top = 18.dp),
            tag = "edit-save",
        )
        Hint("Errors stay inline with your edits. Back with unsaved changes opens a confirmation.")
    }

    if (confirmDiscard) {
        ConfirmModal(
            "Discard unsaved changes?",
            onDismiss = { confirmDiscard = false },
            headerIcon = Icons.Filled.Delete,
            tag = "unsaved-modal",
        ) {
            Text(
                "Your edits have not been saved.",
                color = PennyPalette.muted,
                style = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
                modifier = Modifier.padding(top = 10.dp),
            )
            Notice("Keep editing to save them, or discard them to return to Profile.", Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PennyButton("Discard Changes", { confirmDiscard = false; onBack() }, Modifier.weight(1f), kind = BtnKind.DANGER, tag = "unsaved-discard")
                PennyButton("Keep Editing", { confirmDiscard = false }, Modifier.weight(1f), kind = BtnKind.SECONDARY, tag = "unsaved-keep")
            }
        }
    }
}
