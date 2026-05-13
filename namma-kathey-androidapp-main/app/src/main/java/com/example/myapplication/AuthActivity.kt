package com.example.myapplication

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.auth.AppIconAliasManager
import com.example.myapplication.auth.AuthRepository
import com.example.myapplication.auth.AuthViewModel
import com.example.myapplication.prefs.AppPrefs
import com.example.myapplication.ui.theme.DeepSaffron
import com.example.myapplication.ui.theme.ForestGreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.StorybookCream
import com.example.myapplication.viewmodel.Language
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class AuthActivity : ComponentActivity() {

    private val vm: AuthViewModel by viewModels()

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            vm.handleGoogleIntentResult(it.data)
        }

    private val phoneVerificationHost =
        AuthViewModel.VerificationHost { callbacks, phone ->
            AuthRepository.verifyPhoneNumber(this@AuthActivity, phone, callbacks)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefsUi = getSharedPreferences(AppPrefs.FILE_UI, MODE_PRIVATE)

        setContent {
            var language by remember {
                mutableStateOf(
                    if (prefsUi.getString(AppPrefs.KEY_LANGUAGE_CODE, AppPrefs.LANG_EN) == AppPrefs.LANG_KN) {
                        Language.KN
                    } else {
                        Language.EN
                    },
                )
            }

            val androidCtx = LocalContext.current

            LaunchedEffect(language) {
                val currentSaved = prefsUi.getString(AppPrefs.KEY_LANGUAGE_CODE, AppPrefs.LANG_EN)
                val target = if (language == Language.EN) AppPrefs.LANG_EN else AppPrefs.LANG_KN
                
                if (currentSaved != target) {
                    prefsUi.edit().putString(AppPrefs.KEY_LANGUAGE_CODE, target).apply()
                    // Icon sync is deferred to SplashScreen to prevent immediate app closing
                    
                    val locale = if (language == Language.EN) Locale.ENGLISH else Locale("kn", "IN")
                    Locale.setDefault(locale)
                    val cfg = Configuration(androidCtx.resources.configuration)
                    cfg.setLocale(locale)
                    androidCtx.createConfigurationContext(cfg)
                    
                    // We need to restart activity to apply the new strings from XML resources properly
                    recreate()
                }
            }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { a ->
                    if (a.currentUser != null && !isFinishing) {
                        navigateToMain()
                    }
                }
                FirebaseAuth.getInstance().addAuthStateListener(listener)
                onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
            }

            MyApplicationTheme {
                val snackHost = remember { SnackbarHostState() }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackHost) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { },
                            actions = {
                                IconButton(
                                    onClick = {
                                        language = if (language == Language.EN) Language.KN else Language.EN
                                    },
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null)
                                }
                            },
                        )
                    },
                ) { scaffoldPadding ->
                    AuthBody(
                        contentPadding = scaffoldPadding,
                        snackHost = snackHost,
                        vm = vm,
                        googleLauncher = {
                            googleLauncher.launch(AuthRepository.getGoogleIntent(this@AuthActivity))
                        },
                        phoneHost = phoneVerificationHost,
                        onGuest = {
                            NammaKatheyApplication.userSession(this@AuthActivity).setGuestSession()
                            navigateToMain()
                        },
                    )
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
}

@Composable
private fun AuthBody(
    contentPadding: PaddingValues,
    snackHost: SnackbarHostState,
    vm: AuthViewModel,
    googleLauncher: () -> Unit,
    phoneHost: AuthViewModel.VerificationHost,
    onGuest: () -> Unit,
) {
    val ctx = LocalContext.current
    val route by vm.route.collectAsState()
    val busy by vm.busy.collectAsState()
    val toast by vm.toast.collectAsState()

    LaunchedEffect(toast) {
        val msg = toast?.text ?: return@LaunchedEffect
        snackHost.showSnackbar(msg)
        vm.clearToast()
    }

    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground_art),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(120.dp),
            )
            Text(
                text = stringResource(R.string.auth_welcome_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.auth_welcome_hint),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            val googleOk = AuthRepository.hasWebClientConfigured(ctx)

            when (route) {
                AuthViewModel.Route.Login -> LoginContent(
                    busy = busy,
                    vm = vm,
                    googleLauncher = googleLauncher,
                    googleConfigured = googleOk,
                    onGuest = onGuest,
                    onNavigateRegister = { vm.navigate(AuthViewModel.Route.Register) },
                    onNavigatePhone = { vm.navigate(AuthViewModel.Route.PhoneEnter) },
                )

                AuthViewModel.Route.Register -> RegisterContent(
                    busy = busy,
                    vm = vm,
                    googleLauncher = googleLauncher,
                    googleConfigured = googleOk,
                    onNavigatePhone = { vm.navigate(AuthViewModel.Route.PhoneEnter) },
                    onBack = { vm.navigate(AuthViewModel.Route.Login) },
                )

                AuthViewModel.Route.PhoneEnter -> PhoneEntryContent(
                    busy = busy,
                    vm = vm,
                    host = phoneHost,
                    onBack = { vm.navigate(AuthViewModel.Route.Login) },
                )

                AuthViewModel.Route.PhoneOtp -> OtpEntryContent(
                    busy = busy,
                    vm = vm,
                    onChangeNumber = { vm.navigate(AuthViewModel.Route.PhoneEnter) },
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        if (busy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = DeepSaffron, strokeWidth = 4.dp)
            }
        }
    }
}

@Composable
private fun LoginContent(
    busy: Boolean,
    vm: AuthViewModel,
    googleLauncher: () -> Unit,
    googleConfigured: Boolean,
    onGuest: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigatePhone: () -> Unit,
) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var forgotOpen by remember { mutableStateOf(false) }

    LargeEmailField(email, { email = it }, busy)
    LargePasswordField(password, { password = it }, busy)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { forgotOpen = true }, enabled = !busy) {
            Text(stringResource(R.string.auth_forgot_password), fontWeight = FontWeight.Bold)
        }
    }

    BigPrimary(stringResource(R.string.auth_sign_in), busy) {
        vm.signInEmailPassword(email, password)
    }

    BigTonal(stringResource(R.string.auth_phone_continue), busy, onNavigatePhone)

    BigOutlined(
        label = stringResource(R.string.auth_google_button),
        enabled = !busy,
    ) {
        if (googleConfigured) {
            googleLauncher()
        } else {
            vm.showMessage(ctx.getString(R.string.google_needs_web_client))
        }
    }

    BigGuest(onGuest, busy)

    TextButton(onClick = onNavigateRegister, enabled = !busy) {
        Text(stringResource(R.string.auth_need_account), fontWeight = FontWeight.Bold)
    }

    if (forgotOpen) {
        var resetEmail by remember { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { forgotOpen = false },
            title = { Text(stringResource(R.string.auth_forgot_password)) },
            text = {
                OutlinedTextField(
                    resetEmail,
                    onValueChange = { resetEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_email_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.forgotPassword(resetEmail)
                        forgotOpen = false
                    },
                ) {
                    Text(stringResource(R.string.auth_reset_send))
                }
            },
            dismissButton = {
                TextButton(onClick = { forgotOpen = false }) {
                    Text(stringResource(R.string.auth_cancel))
                }
            },
        )
    }
}

@Composable
private fun RegisterContent(
    busy: Boolean,
    vm: AuthViewModel,
    googleLauncher: () -> Unit,
    googleConfigured: Boolean,
    onNavigatePhone: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }

    LargeEmailField(email, { email = it }, busy)
    LargePasswordField(p1, { p1 = it }, busy)
    OutlinedTextField(
        value = p2,
        onValueChange = { p2 = it },
        label = { Text(stringResource(R.string.auth_confirm_password)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
        enabled = !busy,
        singleLine = true,
        colors = textFieldKidColors(),
    )

    BigPrimary(stringResource(R.string.auth_create_account), busy) {
        vm.registerEmailPassword(email, p1, p2)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Or register with",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BigTonal(stringResource(R.string.auth_phone_continue), busy, onNavigatePhone)
        }
        Column(modifier = Modifier.weight(1f)) {
            BigOutlined(
                label = stringResource(R.string.auth_google_button),
                enabled = !busy,
            ) {
                if (googleConfigured) {
                    googleLauncher()
                } else {
                    vm.showMessage(ctx.getString(R.string.google_needs_web_client))
                }
            }
        }
    }

    TextButton(onClick = onBack, enabled = !busy) {
        Text(stringResource(R.string.auth_have_account))
    }
}

@Composable
private fun PhoneEntryContent(
    busy: Boolean,
    vm: AuthViewModel,
    host: AuthViewModel.VerificationHost,
    onBack: () -> Unit,
) {
    var raw by remember { mutableStateOf("") }
    Text(
        stringResource(R.string.auth_phone_hint_instruction),
        textAlign = TextAlign.Center,
    )
    OutlinedTextField(
        raw,
        onValueChange = { raw = it.filter { ch -> ch.isDigit() || ch == '+' || ch.isWhitespace() } },
        modifier = Modifier
            .fillMaxWidth(),
        label = { Text(stringResource(R.string.auth_mobile_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        enabled = !busy,
        singleLine = true,
        colors = textFieldKidColors(),
    )

    BigPrimary(stringResource(R.string.auth_send_otp), busy, enabledExtra = raw.length >= 8) {
        vm.attachPhoneCallbacks(host, raw)
    }

    TextButton(onClick = onBack, enabled = !busy) {
        Text(stringResource(R.string.auth_back))
    }
}

@Composable
private fun OtpEntryContent(
    busy: Boolean,
    vm: AuthViewModel,
    onChangeNumber: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    Text(stringResource(R.string.auth_otp_hint), textAlign = TextAlign.Center)

    OutlinedTextField(
        code,
        onValueChange = { code = it.filter(Char::isDigit).take(6) },
        label = { Text(stringResource(R.string.auth_otp_digits)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = !busy,
        singleLine = true,
        colors = textFieldKidColors(),
    )

    BigPrimary(stringResource(R.string.auth_verify_otp), busy, enabledExtra = code.length >= 4) {
        vm.verifyOtpEntered(code)
    }

    TextButton(onClick = onChangeNumber, enabled = !busy) {
        Text(stringResource(R.string.auth_change_number))
    }
}

@Composable
private fun textFieldKidColors() = TextFieldDefaults.colors(
    focusedContainerColor = StorybookCream,
    unfocusedContainerColor = StorybookCream.copy(alpha = 0.95f),
    focusedIndicatorColor = ForestGreen,
    unfocusedIndicatorColor = ForestGreen.copy(alpha = 0.5f),
    cursorColor = DeepSaffron,
)

@Composable
private fun LargeEmailField(value: String, onValue: (String) -> Unit, busy: Boolean) {
    OutlinedTextField(
        value,
        onValueChange = onValue,
        label = { Text(stringResource(R.string.auth_email_hint)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        enabled = !busy,
        singleLine = true,
        colors = textFieldKidColors(),
    )
}

@Composable
private fun LargePasswordField(value: String, onValue: (String) -> Unit, busy: Boolean) {
    OutlinedTextField(
        value,
        onValueChange = onValue,
        label = { Text(stringResource(R.string.auth_password_hint)) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        enabled = !busy,
        singleLine = true,
        colors = textFieldKidColors(),
    )
}

@Composable
private fun BigPrimary(
    label: String,
    busy: Boolean,
    enabledExtra: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !busy && enabledExtra,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ForestGreen,
            contentColor = StorybookCream,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BigTonal(label: String, busy: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        enabled = !busy,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = DeepSaffron.copy(alpha = 0.85f),
            contentColor = StorybookCream,
        ),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun BigOutlined(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled,
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BigGuest(onGuest: () -> Unit, busy: Boolean) {
    OutlinedButton(
        onClick = onGuest,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = !busy,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ForestGreen,
        ),
    ) {
        Text(stringResource(R.string.auth_guest_continue), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}
