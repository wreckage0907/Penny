package com.penny.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.R
import com.penny.app.core.AuthFlows
import com.penny.app.core.AuthNext
import com.penny.app.core.FakeAuthService
import com.penny.app.core.NewUser
import com.penny.app.core.ResetResult
import kotlinx.coroutines.launch

/**
 * v2.1 auth (artboard frames 01–03): a revised Sign In with a 48dp password
 * reveal and a complete Account Recovery destination, a Create Account that
 * folds onboarding's identity fields into sign-up (username locked at
 * choice, currency policy explicit), and Google / Phone OTP continues.
 *
 * Superseded v1 quirks (ledger): silent login failure (now an inline
 * error), the GitHub social button (v2.1 IA keeps Google + Phone OTP),
 * and the post-signup onboarding hop for email accounts (onboarding now
 * serves only social/OTP accounts that still need a profile).
 */
class AuthNav(
    val toHome: () -> Unit,
    val toOnboarding: () -> Unit,
    val toSignUp: () -> Unit,
    val toLogin: () -> Unit,
    val toRecovery: () -> Unit = toLogin,
)

@Composable
private fun AuthShell(scrollable: Boolean = true, tag: String, content: @Composable ColumnScopeAlias.() -> Unit) {
    val pad = Modifier.padding(horizontal = 26.dp)
    val column = Modifier
        .fillMaxSize()
        .tag(tag)
        .then(pad)
        .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
        .padding(vertical = 24.dp)
    Column(
        column,
        verticalArrangement = if (scrollable) Arrangement.Top else Arrangement.Center,
    ) { content() }
}

private typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun BrandMark(height: Int = 52) {
    Icon(
        painter = painterResource(R.drawable.title),
        contentDescription = "Penny",
        tint = PennyPalette.brand,
        modifier = Modifier.height(height.dp).fillMaxWidth().padding(horizontal = 40.dp),
    )
}

@Composable
private fun AuthHeading(title: String, subtitle: String) {
    Text(
        title,
        style = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.W800, letterSpacing = (-0.8).sp),
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
    Text(subtitle, color = PennyPalette.muted, style = PennyType.secondaryPlain)
}

// ── 01 · Sign In ───────────────────────────────────────────────────────────

@Composable
fun LoginScreen(services: PennyServices, nav: AuthNav) {
    val scope = rememberCoroutineScope()
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var phoneDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AuthShell(tag = "login-page") {
            BrandMark()
            AuthHeading("Welcome back", "Your money, learning, and next best step.")
            Field(
                "Email or username", id, { id = it; error = null },
                placeholder = "demo@penny.app", tag = "login-id",
            )
            Field(
                "Password", password, { password = it; error = null },
                password = true,
                tag = "login-password",
                trailing = {
                    IconBtn(
                        if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        if (reveal) "Hide password" else "Show password",
                        { reveal = !reveal },
                        bg = androidx.compose.ui.graphics.Color.Transparent,
                        tag = "login-password-toggle",
                    )
                },
            )
            TextLink("Forgot password?", { nav.toRecovery() }, Modifier.padding(top = 6.dp), tag = "login-forgot")
            if (error != null) {
                Notice(error!!, Modifier.padding(top = 12.dp), NoticeKind.NEG, tag = "login-error")
            }
            PennyButton(
                if (busy) "Signing in…" else "Sign In",
                {
                    if (busy) return@PennyButton
                    busy = true
                    error = null
                    scope.launch {
                        val username = AuthFlows.resolveLoginIdentifier(id, services.users)
                        val next = if (username == null) AuthNext.NOTHING else AuthFlows.login(
                            username, password, services.users, services.auth, services.prefs,
                        )
                        busy = false
                        when (next) {
                            AuthNext.HOME -> nav.toHome()
                            AuthNext.NOTHING, AuthNext.ONBOARDING ->
                                error = "Could not sign in. Check your details and try again."
                        }
                    }
                },
                Modifier.padding(top = 12.dp),
                tag = "login-submit",
            )
            Box(Modifier.padding(top = 20.dp)) { TextDivider("or continue with") }
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SocialContinue("Google", Modifier.weight(1f)) { signInSocial(services, nav, scope) }
                SocialContinue("Phone OTP", Modifier.weight(1f)) { phoneDialog = true }
            }
            Row(
                Modifier.padding(top = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("New to Penny? ", color = PennyPalette.muted, style = PennyType.secondaryPlain)
                TextLink("Create account", { nav.toSignUp() }, tag = "login-signup-link")
            }
        }
        if (phoneDialog) {
            PhoneOtpDialog(services, nav, onDismiss = { phoneDialog = false })
        }
    }
}

private fun signInSocial(services: PennyServices, nav: AuthNav, scope: kotlinx.coroutines.CoroutineScope) {
    scope.launch {
        val result = services.auth.signInWithProvider(com.penny.app.core.SocialProvider.GOOGLE)
        when (AuthFlows.afterGoogle(result)) {
            AuthNext.HOME -> nav.toHome()
            AuthNext.ONBOARDING -> nav.toOnboarding()
            AuthNext.NOTHING -> Unit
        }
    }
}

@Composable
private fun SocialContinue(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    PennyButton(label, onClick, modifier, kind = BtnKind.SECONDARY, tag = "social-${label.lowercase().replace(' ', '-')}")
}

/** Phone OTP: verify → code → sign-in; new accounts complete onboarding (v1 parity). */
@Composable
private fun PhoneOtpDialog(services: PennyServices, nav: AuthNav, onDismiss: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ConfirmModal(
        if (sent) "Enter the 6-digit code" else "Sign in with your phone",
        onDismiss = onDismiss,
        tag = if (sent) "code-dialog" else "phone-dialog",
    ) {
        if (error != null) {
            Notice(error!!, Modifier.padding(top = 12.dp), NoticeKind.NEG, tag = "otp-error")
        }
        if (!sent) {
            Field("Phone number", phone, { phone = it }, prefix = "+", keyboardDecimal = true, tag = "phone-input")
            PennyButton(
                "Send code",
                {
                    scope.launch {
                        if (services.auth.verifyPhoneNumber("+${phone.trim()}")) {
                            sent = true; error = null
                        } else {
                            error = "That number doesn't look valid. Use the full international format."
                        }
                    }
                },
                Modifier.padding(top = 14.dp),
                tag = "phone-submit",
            )
        } else {
            Field("6-digit code", code, { code = it }, keyboardDecimal = true, tag = "code-input")
            PennyButton(
                "Verify & continue",
                {
                    scope.launch {
                        if (services.auth.signInWithSmsCode(code.trim())) {
                            nav.toOnboarding()
                        } else {
                            error = "That code doesn't match. Request a new one and try again."
                        }
                    }
                },
                Modifier.padding(top = 14.dp),
                tag = "code-submit",
            )
        }
    }
}

// ── 02 · Account Recovery ─────────────────────────────────────────────────

@Composable
fun RecoveryScreen(services: PennyServices, nav: AuthNav) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    Column(
        Modifier
            .tag("recovery-page")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 5.dp, bottom = 24.dp),
    ) {
        NavBar("Reset password", { nav.toLogin() })
        Text("Check your inbox", style = PennyType.phoneTitle)
        if (!sent) {
            Text(
                "Enter the email on your account and we'll send a 6-digit reset code.",
                color = PennyPalette.muted,
                style = PennyType.secondaryPlain,
                modifier = Modifier.padding(top = 8.dp),
            )
            Field("Email", email, { email = it }, placeholder = "demo@penny.app", tag = "recovery-email")
            PennyButton(
                "Send reset code",
                {
                    scope.launch {
                        if (services.auth.requestPasswordReset(email.trim())) {
                            sent = true
                        } else {
                            codeError = "We couldn't find that account. Check the email and try again."
                        }
                    }
                },
                Modifier.padding(top = 12.dp),
                tag = "recovery-send",
            )
            if (codeError != null) {
                Notice(codeError!!, Modifier.padding(top = 12.dp), NoticeKind.NEG, tag = "recovery-error")
            }
        } else {
            Text(
                "We sent a 6-digit reset code to ${masked(email)}.",
                color = PennyPalette.muted,
                style = PennyType.secondaryPlain,
                modifier = Modifier.padding(top = 8.dp),
            )
            Field(
                "Reset code", code, { code = it; codeError = null },
                keyboardDecimal = true, error = codeError, placeholder = "6 digits",
                tag = "recovery-code",
            )
            TextLink("Resend code", {
                scope.launch {
                    services.auth.requestPasswordReset(email.trim())
                    codeError = "A new code is on its way. The previous one no longer works."
                }
            }, Modifier.padding(top = 2.dp), tag = "recovery-resend")
            Field(
                "New password", newPassword, { newPassword = it; passwordError = null },
                password = true, error = passwordError,
                hint = "Use at least 6 characters. Avoid reusing an old password.",
                tag = "recovery-password",
            )
            PennyButton(
                "Save New Password",
                {
                    scope.launch {
                        when (services.auth.completePasswordReset(email.trim(), code.trim(), newPassword)) {
                            ResetResult.Success -> done = true
                            ResetResult.InvalidCode -> codeError = "That code doesn't match. Check the 6 digits and try again."
                            ResetResult.ExpiredCode -> codeError = "This code expired. Request a new code, then try again."
                            ResetResult.WeakPassword -> passwordError = "Use at least 6 characters."
                        }
                    }
                },
                Modifier.padding(top = 16.dp),
                tag = "recovery-submit",
            )
            if (done) {
                Notice(
                    "Password saved. Return to Sign In — existing sessions were signed out.",
                    Modifier.padding(top = 16.dp), NoticeKind.POS,
                    icon = Icons.Filled.Check, tag = "recovery-done",
                )
                PennyButton("Back to Sign In", { nav.toLogin() }, Modifier.padding(top = 12.dp), tag = "recovery-back")
            } else {
                Notice(
                    "After a successful reset, return to Sign In. Existing sessions will be signed out.",
                    Modifier.padding(top = 18.dp),
                )
            }
        }
    }
}

private fun masked(email: String): String {
    val at = email.indexOf('@')
    if (at <= 0) return email
    return "${email.first()}•••${email.substring(at)}"
}

// ── 03 · Create Account ───────────────────────────────────────────────────

@Composable
fun SignUpScreen(services: PennyServices, nav: AuthNav) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier
            .tag("signup-page")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        BrandMark(58)
        AuthHeading("Create your account", "One profile for your budget and learning progress.")
        Field("Full name", name, { name = it }, tag = "signup-name")
        Field(
            "Choose your username", username, { username = it; usernameError = null },
            prefix = "@", error = usernameError,
            hint = "Your permanent public identity. We check availability before locking it.",
            tag = "signup-username",
        )
        Field("Email", email, { email = it }, tag = "signup-email")
        Field(
            "Password", password, { password = it },
            password = true, hint = "Use at least 6 characters.", tag = "signup-password",
        )
        Field(
            "Default display currency", "Indian Rupee · INR (₹)", {},
            readOnly = true, tag = "signup-currency",
            hint = "Display formatting only. Transactions keep their original amount and currency; no FX conversion is applied.",
        )
        if (formError != null) {
            Notice(formError!!, Modifier.padding(top = 12.dp), NoticeKind.NEG, tag = "signup-error")
        }
        PennyButton(
            if (busy) "Creating…" else "Create Account",
            {
                if (busy) return@PennyButton
                busy = true
                scope.launch {
                    when (AuthFlows.signUpWithProfile(
                        name.trim(), username.trim(), email.trim(), password, services.users, services.auth,
                    )) {
                        com.penny.app.core.AuthFlows.SignUpResult.Home -> nav.toHome()
                        com.penny.app.core.AuthFlows.SignUpResult.UsernameTaken ->
                            usernameError = "That username is already taken. Choose another."
                        com.penny.app.core.AuthFlows.SignUpResult.InvalidUsername ->
                            usernameError = "Usernames can't be empty or contain '/'."
                        com.penny.app.core.AuthFlows.SignUpResult.Failed ->
                            formError = "Could not create your account. Check your details and try again."
                    }
                    busy = false
                }
            },
            Modifier.padding(top = 18.dp),
            tag = "signup-submit",
        )
        Row(
            Modifier.padding(top = 20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Already have an account? ", color = PennyPalette.muted, style = PennyType.secondaryPlain)
            TextLink("Sign In", { nav.toLogin() }, tag = "signup-signin-link")
        }
    }
}

// ── Onboarding (social/OTP completion only, v2.1) ──────────────────────────

@Composable
fun OnboardingScreen(services: PennyServices, nav: AuthNav) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .tag("onboarding-page")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        Text("Complete your profile", style = PennyType.phoneTitle)
        Text(
            "Almost there — tell Penny who you are.",
            color = PennyPalette.muted,
            style = PennyType.secondaryPlain,
            modifier = Modifier.padding(top = 6.dp),
        )
        Field("Username", username, { username = it; error = null }, tag = "onb-username")
        Field("First name", first, { first = it; error = null }, tag = "onb-first")
        Field("Last name", last, { last = it; error = null }, tag = "onb-last")
        Field("Email", email, { email = it; error = null }, tag = "onb-email")
        Field("Phone", phone, { phone = it; error = null }, tag = "onb-phone")
        if (error != null) {
            Notice(error!!, Modifier.padding(top = 12.dp), NoticeKind.NEG, tag = "onb-snackbar")
        }
        PennyButton(
            "Submit",
            {
                scope.launch {
                    val result = AuthFlows.submitOnboarding(
                        NewUser(username.trim(), first.trim(), last.trim(), email.trim(), phone.trim()),
                        services.users, services.auth,
                    )
                    when (result) {
                        is com.penny.app.core.AuthFlows.OnboardingResult.Home -> nav.toHome()
                        is com.penny.app.core.AuthFlows.OnboardingResult.Error -> error = result.snackbar
                    }
                }
            },
            Modifier.padding(top = 18.dp),
            tag = "onb-submit",
        )
    }
}
