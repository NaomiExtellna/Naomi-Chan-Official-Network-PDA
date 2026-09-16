package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.PosViewModel
import com.example.ui.screens.MainPosScreen
import com.example.ui.theme.NaomiChanTheme

class MainActivity : ComponentActivity() {

    private val posViewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NaomiChanTheme(darkTheme = true) {
                MainPosScreen(viewModel = posViewModel)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        posViewModel.onTrimMemory(level)
    }
}

