package com.penny.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.penny.app.PennyServices
import com.penny.app.core.AppRoutes
import com.penny.app.core.AuthFlows
import com.penny.app.core.DashboardFlows
import com.penny.app.core.HomeLogic
import com.penny.app.core.ProfileFlows
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, modifier = Modifier.testTag("profile-title")) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PennyPalette.text)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PennyPalette.background),
    )
}

@Composable
private fun ProfileAvatar(url: String?, size: Dp, tag: String) {
    if (url != null) {
        SubcomposeAsyncImage(
            model = url, contentDescription = "Profile picture",
            modifier = Modifier.size(size).clip(CircleShape).testTag("$tag-image"),
            contentScale = ContentScale.Crop,
            loading = { CircularProgressIndicator() },
            error = { Icon(Icons.Filled.Error, null) },
        )
    } else {
        Box(Modifier.size(size).background(PennyPalette.button, CircleShape).testTag("$tag-default"), Alignment.Center) {
            Icon(Icons.Filled.Person, null, Modifier.size(size / 2), tint = PennyPalette.background)
        }
    }
}

@Composable
private fun WideButton(label: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick, Modifier.fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PennyPalette.button),
    ) { Text(label, fontSize = 18.sp, color = PennyPalette.background, modifier = Modifier.padding(vertical = 8.dp)) }
}

/** ProfileSettings (profile_settings.dart). */
@Composable
fun ProfileSettingsScreen(
    services: PennyServices,
    onBack: () -> Unit,
    navigate: (String) -> Unit,
    toLogin: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var username by remember { mutableStateOf<String?>(null) }
    var fullName by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var passwordDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    // The controllers live on the page and are never cleared (quirk).
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // _loadUserData: Home's rule — claims only when both are present.
        val (u, f) = HomeLogic.identity(services.auth.customClaims(), services.prefs.username(), services.prefs.fullName())
        username = u
        fullName = f
        if (u != null) runCatching { services.profileImages.imageUrl(u) }.onSuccess { imageUrl = it }
    }

    Scaffold(
        containerColor = PennyPalette.background,
        topBar = { ProfileBar("Profile Info", onBack) },
        snackbarHost = { SnackbarHost(snackbar, Modifier.testTag("profile-snackbar")) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceAround) {
                IconButton({ navigate(AppRoutes.HOME) }, Modifier.testTag("profile-nav-home")) {
                    Icon(Icons.Outlined.Home, "Home", Modifier.size(40.dp), tint = PennyPalette.text)
                }
                IconButton({ navigate(AppRoutes.CHATBOT) }, Modifier.testTag("profile-nav-chat")) {
                    Icon(Icons.Outlined.Chat, "Chatbot", Modifier.size(38.dp), tint = PennyPalette.text)
                }
                IconButton({}, enabled = false, modifier = Modifier.testTag("profile-nav-profile")) {
                    Icon(Icons.Rounded.Person, "Profile", Modifier.size(40.dp), tint = PennyPalette.text)
                }
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(20.dp)
            .testTag("profile-page")) {
            Row {
                ProfileAvatar(imageUrl, 80.dp, "profile-avatar")
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(fullName ?: "", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("profile-fullname"))
                    Text(username ?: "", fontSize = 18.sp, color = PennyPalette.text.copy(alpha = 0.7f),
                        modifier = Modifier.testTag("profile-username"))
                }
            }
            Spacer(Modifier.height(30.dp))
            WideButton("Edit Profile", "profile-edit") { navigate(AppRoutes.EDIT_PROFILE) }
            Spacer(Modifier.height(30.dp))
            WideButton("Change Password", "profile-change-password") { passwordDialog = true }
            Spacer(Modifier.height(30.dp))
            WideButton("Sign Out", "profile-sign-out") {
                scope.launch { AuthFlows.signOut(services.auth, services.prefs); toLogin() }
            }
            Spacer(Modifier.height(30.dp))
            WideButton("Delete Account", "profile-delete") { deleteDialog = true }
        }
    }

    if (passwordDialog) {
        AlertDialog(
            onDismissRequest = { passwordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column {
                    listOf(
                        Triple("Current Password", "pw-current", current to { v: String -> current = v }),
                        Triple("New Password", "pw-new", newPassword to { v: String -> newPassword = v }),
                        Triple("Confirm New Password", "pw-confirm", confirm to { v: String -> confirm = v }),
                    ).forEach { (label, tag, binding) ->
                        OutlinedTextField(binding.first, binding.second, Modifier.testTag(tag), label = { Text(label) },
                            visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    }
                }
            },
            confirmButton = {
                TextButton({
                    scope.launch {
                        when (val r = ProfileFlows.changePassword(services.auth, current, newPassword, confirm)) {
                            is ProfileFlows.Outcome.Done -> { passwordDialog = false; snackbar.showSnackbar(r.message) }
                            is ProfileFlows.Outcome.Failed -> snackbar.showSnackbar(r.message)
                            ProfileFlows.Outcome.Silent -> Unit
                        }
                    }
                }, Modifier.testTag("pw-change")) { Text("Change") }
            },
            dismissButton = { TextButton({ passwordDialog = false }, Modifier.testTag("pw-cancel")) { Text("Cancel") } },
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
            confirmButton = {
                TextButton({
                    deleteDialog = false
                    scope.launch {
                        when (val r = ProfileFlows.deleteAccount(services.auth, services.users, services.prefs, username, imageUrl)) {
                            is ProfileFlows.Outcome.Done -> toLogin()
                            is ProfileFlows.Outcome.Failed -> snackbar.showSnackbar(r.message)
                            ProfileFlows.Outcome.Silent -> Unit
                        }
                    }
                }, Modifier.testTag("delete-account-confirm")) { Text("Delete", color = Color.Red) }
            },
            dismissButton = { TextButton({ deleteDialog = false }, Modifier.testTag("delete-account-cancel")) { Text("Cancel") } },
        )
    }
}

/** EditProfile (edit_profile.dart). */
@Composable
fun EditProfileScreen(services: PennyServices, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val toasts = remember { mutableStateListOf<Pair<ProfileFlows.Toast, String>>() }
    var username by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var pending by remember { mutableStateOf<CompletableDeferred<String?>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pending?.complete(uri?.toString())
    }
    suspend fun pick(): String? = services.pickImage?.invoke() ?: CompletableDeferred<String?>().also {
        pending = it
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }.await()

    suspend fun loadImage() {
        username?.let { u -> runCatching { services.profileImages.imageUrl(u) }.onSuccess { imageUrl = it } }
    }

    suspend fun loadFields() {
        val f = ProfileFlows.fields(username?.let { u -> runCatching { services.users.getUser(u) }.getOrNull() })
        firstName = f.firstName; lastName = f.lastName; email = f.email; phone = f.phone
    }

    fun show(message: String) { scope.launch { snackbar.showSnackbar(message) } }

    LaunchedEffect(Unit) {
        // _loadUsername: the username claim, else prefs (like the dashboard).
        username = DashboardFlows.username(services.auth.customClaims(), services.prefs.username())
        if (username != null) { loadImage(); loadFields() }
    }

    Scaffold(
        containerColor = PennyPalette.background,
        topBar = { ProfileBar("Edit Profile", onBack) },
        snackbarHost = { SnackbarHost(snackbar, Modifier.testTag("edit-snackbar")) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("edit-page")) {
                Box(Modifier.align(Alignment.CenterHorizontally)) {
                    ProfileAvatar(imageUrl, 100.dp, "edit-avatar")
                    IconButton(
                        onClick = {
                            scope.launch {
                                when (val r = ProfileFlows.updatePicture(services.profileImages, username, pick())) {
                                    is ProfileFlows.Outcome.Done -> { show(r.message); loadImage() }
                                    is ProfileFlows.Outcome.Failed -> show(r.message)
                                    ProfileFlows.Outcome.Silent -> Unit
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).size(36.dp).background(PennyPalette.card, CircleShape)
                            .testTag("edit-picture"),
                    ) { Icon(Icons.Filled.Edit, "Change picture", Modifier.size(18.dp), tint = PennyPalette.text) }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        scope.launch {
                            when (val r = ProfileFlows.deletePicture(services.profileImages, username)) {
                                is ProfileFlows.Outcome.Done -> { show(r.message); imageUrl = null }
                                is ProfileFlows.Outcome.Failed -> show(r.message)
                                ProfileFlows.Outcome.Silent -> Unit
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally).testTag("edit-delete-picture"),
                ) { Text("Delete Profile Picture", color = Color.Red) }
                LabelledField("Username", ProfileFlows.usernameField(username), {}, "edit-username", readOnly = true)
                LabelledField("First Name", firstName, { firstName = it }, "edit-first")
                LabelledField("Last Name", lastName, { lastName = it }, "edit-last")
                LabelledField("Email ID", email, { email = it }, "edit-email")
                LabelledField("Phone Number", phone, { phone = it }, "edit-phone")
                Spacer(Modifier.height(30.dp))
                WideButton("Save Changes", "edit-save") {
                    scope.launch {
                        val fields = ProfileFlows.Fields(firstName, lastName, email, phone)
                        when (val r = ProfileFlows.saveProfile(services.auth, services.users, services.prefs, username, fields)) {
                            is ProfileFlows.SaveResult.Toasts -> { toasts.addAll(r.toasts); loadFields() }
                            is ProfileFlows.SaveResult.SnackBar -> show(r.message)
                            ProfileFlows.SaveResult.Silent -> Unit
                        }
                    }
                }
            }
            ToastHost(toasts, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun LabelledField(label: String, value: String, onChange: (String) -> Unit, tag: String, readOnly: Boolean = false) {
    Spacer(Modifier.height(20.dp))
    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = PennyPalette.text)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth().testTag(tag), readOnly = readOnly, singleLine = true)
}

/** toastification stand-in: stacked, colour-coded, auto-dismissed after 4 s. */
@Composable
fun ToastHost(toasts: MutableList<Pair<ProfileFlows.Toast, String>>, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        toasts.forEachIndexed { i, (kind, text) ->
            val color = when (kind) {
                ProfileFlows.Toast.SUCCESS -> Color(0xFF4CAF50)
                ProfileFlows.Toast.ERROR -> Color(0xFFF44336)
                ProfileFlows.Toast.INFO -> Color(0xFF2196F3)
                ProfileFlows.Toast.WARNING -> Color(0xFFFF9800)
            }
            Text(text, Modifier.shadow(4.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
                .background(Color.White).background(color.copy(alpha = 0.15f)) // flatColored: opaque, tinted
                .padding(horizontal = 16.dp, vertical = 10.dp).testTag("toast-${kind.name.lowercase()}"),
                color = color, fontWeight = FontWeight.SemiBold)
        }
    }
    LaunchedEffect(toasts.size) {
        if (toasts.isNotEmpty()) {
            delay(4_000)
            toasts.clear()
        }
    }
}
