package com.penny.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.R
import com.penny.app.core.AuthFlows
import com.penny.app.core.AuthNext
import com.penny.app.core.NewUser
import com.penny.app.core.SocialProvider
import kotlinx.coroutines.launch

/** Navigation targets the auth pages use (Flutter pushNamed equivalents). */
class AuthNav(
    val toHome: () -> Unit,
    val toOnboarding: () -> Unit,
    val toSignUp: () -> Unit,
    val toLogin: () -> Unit,
)

private fun AuthNav.go(next: AuthNext) = when (next) {
    AuthNext.HOME -> toHome()
    AuthNext.ONBOARDING -> toOnboarding()
    AuthNext.NOTHING -> Unit // Flutter only prints
}

/** LoginPage (login.dart): username + password, social row, sign-up link. */
@Composable
fun LoginScreen(services: PennyServices, nav: AuthNav) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AuthPage(tag = "login-page", title = "Welcome Back!") {
        UnderlineField(username, { username = it }, "Username", Icons.Outlined.Person, "login-username")
        Spacer(Modifier.height(14.dp))
        PasswordField(password, { password = it }, "login-password")
        Spacer(Modifier.height(25.dp))
        SocialRow(services, nav, onLoginPage = true)
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Sign In", "login-submit") {
            scope.launch {
                nav.go(AuthFlows.login(username, password, services.users, services.auth, services.prefs))
            }
        }
        Spacer(Modifier.height(30.dp))
        SwitchLink("Don't have an account? ", "Sign Up", "login-signup-link", nav.toSignUp)
    }
}

/** SignUpPage (signup.dart): email + password, social row, sign-in link. */
@Composable
fun SignUpScreen(services: PennyServices, nav: AuthNav) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AuthPage(tag = "signup-page", title = "Welcome!") {
        UnderlineField(
            email, { email = it }, "Email", Icons.Outlined.Email, "signup-email",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(14.dp))
        PasswordField(password, { password = it }, "signup-password")
        Spacer(Modifier.height(25.dp))
        SocialRow(services, nav, onLoginPage = false)
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Sign Up", "signup-submit") {
            scope.launch { nav.go(AuthFlows.signUp(email, password, services.auth)) }
        }
        Spacer(Modifier.height(30.dp))
        SwitchLink("Already have an account? ", "Sign In", "signup-signin-link", nav.toLogin)
    }
}

/** OnboardingPage (onboarding_page.dart): profile form → Home. */
@Composable
fun OnboardingScreen(services: PennyServices, nav: AuthNav) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar, Modifier.testTag("onb-snackbar")) },
        containerColor = PennyPalette.background,
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(28.dp)
                .testTag("onboarding-page"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Let's Get Started!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text)
            Spacer(Modifier.height(20.dp))
            // Avatar picker: image upload (POST /prof) is not mimicked yet.
            Box(
                Modifier.size(100.dp).background(Color(0xFFE0E0E0), CircleShape).testTag("onb-avatar"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.AddAPhoto, null, Modifier.size(40.dp), tint = Color(0xFF757575))
            }
            listOf(
                Triple("Username", "onb-username", username to { v: String -> username = v }),
                Triple("First Name", "onb-first", firstName to { v: String -> firstName = v }),
                Triple("Last Name", "onb-last", lastName to { v: String -> lastName = v }),
                Triple("Email", "onb-email", email to { v: String -> email = v }),
                Triple("Phone No.", "onb-phone", phone to { v: String -> phone = v }),
            ).forEach { (label, tag, binding) ->
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    binding.first, binding.second, Modifier.fillMaxWidth().testTag(tag),
                    label = { Text(label) }, singleLine = true,
                )
            }
            Spacer(Modifier.height(15.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(
                    onClick = {
                        scope.launch {
                            val user = NewUser(username, firstName, lastName, email, phone)
                            when (val r = AuthFlows.submitOnboarding(user, services.users, services.auth)) {
                                AuthFlows.OnboardingResult.Home -> nav.toHome()
                                is AuthFlows.OnboardingResult.Error -> snackbar.showSnackbar(r.snackbar)
                            }
                        }
                    },
                    modifier = Modifier.testTag("onb-submit"),
                ) {
                    Text("Submit", fontSize = 22.sp, color = PennyPalette.text)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = PennyPalette.text)
                }
            }
        }
    }
}

@Composable
private fun AuthPage(tag: String, title: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = PennyPalette.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp).testTag(tag),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(painterResource(R.drawable.title), "Penny", Modifier.width(200.dp))
            Spacer(Modifier.height(20.dp))
            Text(title, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text,
                modifier = Modifier.testTag("$tag-title"))
            Spacer(Modifier.height(26.dp))
            content()
        }
    }
}

@Composable
private fun UnderlineField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    icon: ImageVector,
    tag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    TextField(
        value, onChange, Modifier.fillMaxWidth().testTag(tag),
        placeholder = { Text(hint) },
        leadingIcon = { Icon(icon, null, Modifier.size(36.dp), tint = PennyPalette.text) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
}

/** PasswordField (input_field.dart:59-105): obscured by default, eye toggle. */
@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, tag: String) {
    var obscured by remember { mutableStateOf(true) }
    TextField(
        value, onChange, Modifier.fillMaxWidth().testTag(tag),
        placeholder = { Text("Password") },
        leadingIcon = {
            Icon(if (obscured) Icons.Outlined.Lock else Icons.Outlined.LockOpen, null, Modifier.size(36.dp),
                tint = PennyPalette.text)
        },
        trailingIcon = {
            IconButton(onClick = { obscured = !obscured }, modifier = Modifier.testTag("$tag-toggle")) {
                Icon(if (obscured) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null,
                    tint = PennyPalette.text)
            }
        },
        visualTransformation = if (obscured) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
}

/** "or continue with" + Google / GitHub / phone (login.dart:100-178). */
@Composable
private fun SocialRow(services: PennyServices, nav: AuthNav, onLoginPage: Boolean) {
    val scope = rememberCoroutineScope()
    var phoneDialog by remember { mutableStateOf(false) }
    var codeDialog by remember { mutableStateOf(false) }
    Text("or continue with", fontSize = 16.sp, color = PennyPalette.text)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SocialButton("social-google", "G") {
            scope.launch { nav.go(AuthFlows.afterGoogle(services.auth.signInWithProvider(SocialProvider.GOOGLE))) }
        }
        SocialButton("social-github", "GH") {
            scope.launch {
                val result = services.auth.signInWithProvider(SocialProvider.GITHUB)
                nav.go(AuthFlows.afterGithub(result, onLoginPage))
            }
        }
        SocialButton("social-phone", null) { phoneDialog = true }
    }
    if (phoneDialog) {
        InputDialog("Enter your phone number", "Phone Number", "phone-dialog", KeyboardType.Phone,
            onDismiss = { phoneDialog = false }) { number ->
            phoneDialog = false
            scope.launch { if (services.auth.verifyPhoneNumber(number)) codeDialog = true }
        }
    }
    if (codeDialog) {
        InputDialog("Enter the verification code", "Verification Code", "code-dialog", KeyboardType.Number,
            onDismiss = { codeDialog = false }) { code ->
            codeDialog = false
            // _handleRouting: isFirstTime is a hard-coded true → always onboarding.
            scope.launch { if (services.auth.signInWithSmsCode(code)) nav.toOnboarding() }
        }
    }
}

@Composable
private fun SocialButton(tag: String, label: String?, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).background(PennyPalette.card, CircleShape).clickable(onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PennyPalette.text)
        } else {
            Icon(Icons.Filled.Phone, "Phone", tint = PennyPalette.text)
        }
    }
}

@Composable
private fun InputDialog(
    title: String,
    label: String,
    tag: String,
    keyboardType: KeyboardType,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(text, { text = it }, Modifier.testTag("$tag-input"), label = { Text(label) },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = true)
        },
        confirmButton = { TextButton({ onSubmit(text) }, Modifier.testTag("$tag-submit")) { Text("Submit") } },
        dismissButton = { TextButton(onDismiss, Modifier.testTag("$tag-cancel")) { Text("Cancel") } },
    )
}

@Composable
private fun PrimaryButton(label: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick,
        Modifier.fillMaxWidth().padding(horizontal = 40.dp).testTag(tag),
        shape = RoundedCornerShape(34.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PennyPalette.button),
    ) {
        Text(label, fontSize = 20.sp, color = PennyPalette.background, modifier = Modifier.padding(vertical = 6.dp))
    }
}

@Composable
private fun SwitchLink(prefix: String, link: String, tag: String, onClick: () -> Unit) {
    Text(
        buildAnnotatedString {
            append(prefix)
            withStyle(SpanStyle(color = PennyPalette.button, textDecoration = TextDecoration.Underline)) { append(link) }
        },
        fontSize = 16.sp,
        color = Color.Black,
        modifier = Modifier.clickable(onClick = onClick).testTag(tag),
    )
}
