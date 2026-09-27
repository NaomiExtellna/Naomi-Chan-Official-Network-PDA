package com.example

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AuthViewModel
import com.example.ui.WarehouseViewModel
import com.example.ui.screens.ChangeCredentialScreen
import com.example.ui.screens.NaomiSplashLoadingScreen
import com.example.ui.screens.RecoveryCodeNoticeScreen
import com.example.ui.screens.StaffAccessScreen
import com.example.ui.screens.WarehouseAppScreen
import com.example.ui.theme.NaomiChanTheme
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import com.example.util.DiagnosticLog
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        private const val MIN_SPLASH_DURATION_MS = 350L
    }

    private val warehouseViewModel: WarehouseViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this)[WarehouseViewModel::class.java]
    }

    private val authViewModel: AuthViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this)[AuthViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DiagnosticLog.installCrashHandler(this)

        val authVmAttempt = runCatching { authViewModel }
        val authVm = authVmAttempt.getOrNull()
        val authVmError = authVmAttempt.exceptionOrNull()

        // Keep the classic inset path for SUNMI OS / Android 7.1.x reliability.
        setContent {
            NaomiChanTheme {
                var minimumSplashElapsed by remember { mutableStateOf(false) }
                val authState = authVm?.state?.collectAsStateWithLifecycle()?.value

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
                    DiagnosticLog.error(
                        this@MainActivity,
                        "MainActivity/AuthViewModel",
                        authVmError
                    )
                }
            }
            StartupFailureScreen(
                message = authVmError?.let {
                    it.javaClass.simpleName + ": " + (it.message ?: "Unknown startup error")
                } ?: "Authentication/database startup failed.",
                onRetry = { recreate() }
            )
            return
        }

        val authState by authVm.state.collectAsStateWithLifecycle()
        val user = authState.currentUser
        val recoveryCode = authState.pendingRecoveryCode

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
                val warehouseVmResult = remember { runCatching { warehouseViewModel } }
                val warehouseVm = warehouseVmResult.getOrNull()

                if (warehouseVm == null) {
                    val error = warehouseVmResult.exceptionOrNull()
                    LaunchedEffect(error) {
                        if (error != null) {
                            DiagnosticLog.error(
                                this@MainActivity,
                                "MainActivity/WarehouseViewModel",
                                error
                            )
                        }
                    }
                    StartupFailureScreen(
                        message = error?.let {
                            it.javaClass.simpleName + ": " +
                                (it.message ?: "Warehouse startup error")
                        } ?: "BFC warehouse startup failed.",
                        onRetry = { recreate() }
                    )
                    return
                }

                LaunchedEffect(user, authState.activeShift) {
                    warehouseVm.setOperator(
                        staffId = user.id,
                        displayName = user.displayName,
                        shiftId = authState.activeShift?.id
                    )
                }

                WarehouseAppScreen(
                    viewModel = warehouseVm,
                    authViewModel = authVm
                )
            }
        }
    }

    @Composable
    private fun StartupFailureScreen(
        message: String,
        onRetry: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NaomiDarkBg)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Naomi-Chan BFC Warehouse could not finish starting",
                color = NaomiTextPrimary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = message,
                color = NaomiTextSecondary,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
            Text(
                text = "This error has also been written to pda_diagnostics.log.",
                color = NaomiOrange,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
