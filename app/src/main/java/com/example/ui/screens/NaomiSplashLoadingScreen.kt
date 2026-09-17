package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun NaomiSplashLoadingScreen() {
    val legacySunmi = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF35120D),
                        Color(0xFF7B2A1B),
                        Color(0xFFC73D24)
                    )
                )
            )
    ) {
        // The SUNMI V2 runs Android 7.1.1 / API 25. Its resource decoder throws a
        // ResourceResolutionException when Compose attempts to load the JPEG splash
        // from drawable-nodpi. Keep the bitmap splash for newer Android devices and
        // use a fully Compose-rendered background on API 25 and below.
        if (!legacySunmi) {
            Image(
                painter = painterResource(id = R.drawable.naomi_splash_bg),
                contentDescription = "Naomi-Chan loading screen",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xEFFFF7EC))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFFC73D24),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Naomi-Chan PDA",
                color = Color(0xFF7B2A1B),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Text(
                text = "Loading Staff Terminal…",
                color = Color(0xFF865B48),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}
