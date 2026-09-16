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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.util.DiagnosticLog
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        private const val AUTO_LOCK_AFTER_MS = 5 * 60 * 1000L
        private const val MIN_SPLASH_DURATION_MS = 1_200L
        private const val PERMISSION_REQUEST_DELAY_MS = 350L
    }

    private var posViewModelInitialized = false
    private val posViewModel: PosViewModel by lazy(LazyThreadSafetyMode.NONE) {
        posViewModelInitialized = true
        ViewModelProvider(this)[PosViewModel::class.java]
    }
    private val authViewModel: AuthViewModel by viewModels()
    private var backgroundedAt: Long = 0L

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Do not initialize the POS/printer stack from a permission callback.
        // Printer discovery will refresh when the POS UI is actually opened.
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
                val authState by authViewModel.state.collectAsState()
                val user = authState.currentUser
                val recoveryCode = authState.pendingRecoveryCode
                var minimumSplashElapsed by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(MIN_SPLASH_DURATION_MS)
                    minimumSplashElapsed = true
                    delay(PERMISSION_REQUEST_DELAY_MS)
                    requestBluetoothPermissionsIfNeeded()
                }

                when {
                    !minimumSplashElapsed || authState.isLoading -> NaomiSplashLoadingScreen()
                    recoveryCode != null -> RecoveryCodeNoticeScreen(
                        code = recoveryCode,
                        onAcknowledge = authViewModel::acknowledgeRecoveryCode
                    )
                    user == null -> StaffAccessScreen(
                        state = authState,
                        onCreateAdmin = authViewModel::createNaomiAdmin,
                        onLogin = authViewModel::login,
                        onRegister = authViewModel::registerStaff,
                        onRecoverAdmin = authViewModel::recoverNaomiAdmin
                    )
                    user.mustChangeCredential -> ChangeCredentialScreen(
                        displayName = user.displayName,
                        onChange = authViewModel::changeOwnCredential
                    )
                    else -> {
                        LaunchedEffect(user, authState.activeShift) {
                            posViewModel.setOperator(
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
                        MainPosScreen(viewModel = posViewModel, authViewModel = authViewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (backgroundedAt > 0L && System.currentTimeMillis() - backgroundedAt >= AUTO_LOCK_AFTER_MS) {
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
