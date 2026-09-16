package com.example

import android.Manifest
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
import androidx.core.content.ContextCompat
import com.example.ui.AuthViewModel
import com.example.ui.PosViewModel
import com.example.ui.screens.MainPosScreen
import com.example.ui.screens.StaffAccessScreen
import com.example.ui.theme.NaomiChanTheme

class MainActivity : ComponentActivity() {

    private val posViewModel: PosViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        posViewModel.printerManager.refreshDiscoveredDevices()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestBluetoothPermissionsIfNeeded()

        setContent {
            NaomiChanTheme(darkTheme = true) {
                val authState by authViewModel.state.collectAsState()

                if (authState.currentUser == null) {
                    StaffAccessScreen(
                        state = authState,
                        onCreateAdmin = authViewModel::createNaomiAdmin,
                        onLogin = authViewModel::login,
                        onRegister = authViewModel::registerStaff
                    )
                } else {
                    LaunchedEffect(authState.currentUser, authState.activeShift) {
                        val user = authState.currentUser
                        posViewModel.setOperator(
                            displayName = user?.displayName.orEmpty(),
                            shiftId = authState.activeShift?.id
                        )
                    }

                    MainPosScreen(
                        viewModel = posViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

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
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        posViewModel.onTrimMemory(level)
    }
}
