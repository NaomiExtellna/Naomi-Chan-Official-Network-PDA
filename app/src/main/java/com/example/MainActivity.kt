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
import androidx.core.content.ContextCompat
import com.example.ui.PosViewModel
import com.example.ui.screens.MainPosScreen
import com.example.ui.theme.NaomiChanTheme

class MainActivity : ComponentActivity() {

    private val posViewModel: PosViewModel by viewModels()

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Refresh after the user grants/denies the Android 12+ Bluetooth permissions.
        posViewModel.printerManager.refreshDiscoveredDevices()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestBluetoothPermissionsIfNeeded()

        setContent {
            NaomiChanTheme(darkTheme = true) {
                MainPosScreen(viewModel = posViewModel)
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
