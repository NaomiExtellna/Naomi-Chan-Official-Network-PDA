package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.ContextCompat
import com.example.ui.AuthViewModel
import com.example.ui.PosViewModel
import com.example.ui.screens.ChangeCredentialScreen
import com.example.ui.screens.MainPosScreen
import com.example.ui.screens.NaomiSplashLoadingScreen
import com.example.ui.screens.RecoveryCodeNoticeScreen
import com.example.ui.screens.StaffAccessScreen
import com.example.ui.theme.NaomiChanTheme
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import com.example.util.DiagnosticLog
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        private const val AUTO_LOCK_AFTER_MS = 5 * 60 * 1000L
        private const val MIN_SPLASH_DURATION_MS = 1_200L
        private const val PERMISSION_REQUEST_DELAY_MS = 350L
    }

    private var posViewModelInitialized = false
    private var authViewModelInitialized = false

    private val posViewModel: PosViewModel by lazy(LazyThreadSafetyMode.NONE) {
        posViewModelInitialized = true
        ViewModelProvider(this)[PosViewModel::class.java]
    }

    private val authViewModel: AuthViewModel by lazy(LazyThreadSafetyMode.NONE) {
        authViewModelInitialized = true
        ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private var backgroundedAt: Long = 0L

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Do not initialize the POS/printer stack from a permission callback.
        // Hardware discovery happens only once the POS UI is opened.
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        DiagnosticLog.installCrashHandler(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NaomiChanTheme(darkTheme = true) {
                var splashComplete by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(MIN_SPLASH_DURATION_MS)
                    splashComplete = true
                }

                if (!splashComplete) {
                    NaomiSplashLoadingScreen()
                } else {
                    AppAfterSplash()
                }
            }
        }
    }

    @Composable
    private fun AppAfterSplash() {
        var startupFailure by remember { mutableStateOf<String?>(null) }
        val authVm = remember {
            runCatching { authViewModel }
                .onFailure { error ->
                    DiagnosticLog.error(this, "MainActivity/AuthViewModel", error)
                    startupFailure = "${error.javaClass.simpleName}: ${error.message ?: "Unknown startup error"}"
                }
                .getOrNull()
        }

        if (authVm == null) {
            StartupFailureScreen(
                message = startupFailure ?: "Authentication/database startup failed.",
                onRetry = { recreate() }
            )
            return
        }

        val authState by authVm.state.collectAsState()
        val user = authState.currentUser
        val recoveryCode = authState.pendingRecoveryCode

        LaunchedEffect(Unit) {
            delay(PERMISSION_REQUEST_DELAY_MS)
            requestBluetoothPermissionsIfNeeded()
        }

        authState.startupError?.let { error ->
            StartupFailureScreen(
                message = error,
                onRetry = { recreate() }
            )
            return
        }

        when {
            authState.isLoading -> NaomiSplashLoadingScreen()
            recoveryCode != null -> RecoveryCodeNoticeScreen(
                code = recoveryCode,
                onAcknowledge = authVm::acknowledgeRecoveryCode
            )
            user == null -> StaffAccessScreen(
                state = authState,
                onCreateAdmin = authVm::createNaomiAdmin,
                onLogin = authVm::login,
                onRegister = authVm::registerStaff,
                onRecoverAdmin = authVm::recoverNaomiAdmin
            )
            user.mustChangeCredential -> ChangeCredentialScreen(
                displayName = user.displayName,
                onChange = authVm::changeOwnCredential
            )
            else -> {
                val posVmResult = remember {
                    runCatching { posViewModel }
                        .onFailure { error ->
                            DiagnosticLog.error(this, "MainActivity/PosViewModel", error)
                            startupFailure = "${error.javaClass.simpleName}: ${error.message ?: "POS startup error"}"
                        }
                        .getOrNull()
                }
                val posVm = posVmResult
                if (posVm == null) {
                    StartupFailureScreen(
                        message = startupFailure ?: "POS/printer startup failed.",
                        onRetry = { recreate() }
                    )
                    return
                }

                LaunchedEffect(user, authState.activeShift) {
                    posVm.setOperator(
                        staffId = user.id,
                        displayName = user.displayName,
                        shiftId = authState.activeShift?.id,
                        isAdmin = user.isAdmin,
                        canVoid = user.canVoid,
                        canExport = user.canExport,
                        canEditVenues = user.canEditVenues,
                        canChangeGateway = user.canChangeGateway,
                        canViewTotals = user.canViewTotals
                    )
                }
                MainPosScreen(viewModel = posVm, authViewModel = authVm)
            }
        }
    }

    @Composable
    private fun StartupFailureScreen(message: String, onRetry: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NaomiDarkBg)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Naomi-Chan PDA could not finish starting",
                color = NaomiTextPrimary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = message,
                color = NaomiTextSecondary,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
            )
            Button(onClick = onRetry) {
                Text("Retry", color = NaomiTextPrimary)
            }
            Text(
                text = "This error has also been written to pda_diagnostics.log.",
                color = NaomiOrange,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (
            authViewModelInitialized &&
            backgroundedAt > 0L &&
            System.currentTimeMillis() - backgroundedAt >= AUTO_LOCK_AFTER_MS
        ) {
            authViewModel.logout()
        }
        backgroundedAt = 0L
    }

    override fun onStop() {
        backgroundedAt = System.currentTimeMillis()
        super.onStop()
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        runCatching {
            val requiredPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val missingPermissions = requiredPermissions.filter { permission ->
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
            }
            if (missingPermissions.isNotEmpty()) {
                bluetoothPermissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }.onFailure { error ->
            DiagnosticLog.error(this, "MainActivity/bluetoothPermission", error)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (posViewModelInitialized) {
            posViewModel.onTrimMemory(level)
        }
    }
}
