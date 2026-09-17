package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
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
        private const val MIN_SPLASH_DURATION_MS = 350L
        private const val PERMISSION_REQUEST_DELAY_MS = 350L
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 7301
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install diagnostics only after Activity creation. Keeping attachBaseContext untouched
        // avoids doing custom work in the fragile pre-onCreate path on SUNMI OS / Android 7.1.1.
        DiagnosticLog.installCrashHandler(this)

        // Start local authentication initialization immediately instead of waiting for the
        // cosmetic splash to finish. This allows Room startup and the minimum splash window
        // to overlap rather than running sequentially.
        val authVmAttempt = runCatching { authViewModel }
        val authVm = authVmAttempt.getOrNull()
        val authVmError = authVmAttempt.exceptionOrNull()

        // Deliberately do not call enableEdgeToEdge() here. The SUNMI V2 runs API 25 and
        // its customised SystemUI is more reliable with the classic window-inset path.
        setContent {
            NaomiChanTheme(darkTheme = true) {
                var minimumSplashElapsed by remember { mutableStateOf(false) }
                val authState = authVm?.state?.collectAsState()?.value

                LaunchedEffect(Unit) {
                    delay(MIN_SPLASH_DURATION_MS)
                    minimumSplashElapsed = true
                }

                val authenticationReady = authVmError != null || authState?.isLoading == false
                if (!minimumSplashElapsed || !authenticationReady) {
                    NaomiSplashLoadingScreen()
                } else {
                    AppAfterSplash(authVm = authVm, authVmError = authVmError)
                }
            }
        }
    }

    @Composable
    private fun AppAfterSplash(authVm: AuthViewModel?, authVmError: Throwable?) {
        if (authVm == null) {
            LaunchedEffect(authVmError) {
                if (authVmError != null) {
                    DiagnosticLog.error(this@MainActivity, "MainActivity/AuthViewModel", authVmError)
                }
            }
            StartupFailureScreen(
                message = authVmError?.let { "${it.javaClass.simpleName}: ${it.message ?: "Unknown startup error"}" }
                    ?: "Authentication/database startup failed.",
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
                val posVmResult = remember { runCatching { posViewModel } }
                val posVm = posVmResult.getOrNull()
                if (posVm == null) {
                    val error = posVmResult.exceptionOrNull()
                    LaunchedEffect(error) {
                        if (error != null) {
                            DiagnosticLog.error(this@MainActivity, "MainActivity/PosViewModel", error)
                        }
                    }
                    StartupFailureScreen(
                        message = error?.let { "${it.javaClass.simpleName}: ${it.message ?: "POS startup error"}" }
                            ?: "POS/printer startup failed.",
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
        // Android 7.1.1 (API 25) uses the manifest-granted BLUETOOTH and
        // BLUETOOTH_ADMIN permissions and must never enter the Android 12 flow.
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
                requestPermissions(
                    missingPermissions.toTypedArray(),
                    BLUETOOTH_PERMISSION_REQUEST_CODE
                )
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
