package com.example.ui.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.os.BatteryManager
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val AD_ROTATION_MS = 12_000L
private const val POWER_SAVE_AD_ROTATION_MS = 30_000L
private const val CLOCK_TICK_MS = 60_000L

@Composable
fun IdleAttractScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    DisposableEffect(activity, context) {
        val window = activity?.window
        val decorView = window?.decorView
        val previousSystemUi = decorView?.systemUiVisibility ?: 0
        val previousStatusBarColor = window?.statusBarColor
        val previousNavigationBarColor = window?.navigationBarColor
        val keepScreenOnWasSet = window?.attributes?.flags
            ?.and(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            ?.let { it != 0 }
            ?: false

        if (decorView != null) {
            decorView.systemUiVisibility = previousSystemUi or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        window?.statusBarColor = AndroidColor.TRANSPARENT
        window?.navigationBarColor = AndroidColor.TRANSPARENT

        fun applyChargingPolicy(intent: Intent?) {
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0

            // Bedside-clock mode stays awake while plugged in. On battery, respect the
            // terminal's normal screen timeout instead of silently draining it overnight.
            if (charging || keepScreenOnWasSet) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                applyChargingPolicy(intent)
            }
        }

        var receiverRegistered = false
        val stickyBatteryIntent = runCatching {
            val sticky = context.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            receiverRegistered = true
            sticky
        }.getOrNull()
        applyChargingPolicy(stickyBatteryIntent)

        onDispose {
            if (receiverRegistered) {
                runCatching { context.unregisterReceiver(batteryReceiver) }
            }
            if (decorView != null) decorView.systemUiVisibility = previousSystemUi
            if (previousStatusBarColor != null) window?.statusBarColor = previousStatusBarColor
            if (previousNavigationBarColor != null) window?.navigationBarColor = previousNavigationBarColor

            // Restore the exact screen-on policy that existed before attract mode opened.
            if (keepScreenOnWasSet) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val adResourceIds = remember {
        listOf(
            R.drawable.promo_ad_01,
            R.drawable.promo_ad_02,
            R.drawable.promo_ad_03,
            R.drawable.promo_ad_04
        )
    }

    var adIndex by remember { mutableIntStateOf(0) }
    var clockTick by remember { mutableIntStateOf(0) }
    val tapInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(adResourceIds.size, powerManager) {
        while (adResourceIds.size > 1) {
            val rotationDelay = if (powerManager?.isPowerSaveMode == true) {
                POWER_SAVE_AD_ROTATION_MS
            } else {
                AD_ROTATION_MS
            }
            delay(rotationDelay)
            adIndex = (adIndex + 1) % adResourceIds.size
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            clockTick = (now / CLOCK_TICK_MS).toInt()
            delay((CLOCK_TICK_MS - (now % CLOCK_TICK_MS)).coerceAtLeast(1_000L))
        }
    }

    val time = remember(clockTick) {
        SimpleDateFormat("HH:mm", Locale.UK).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = tapInteraction,
                indication = null,
                onClick = onDismiss
            )
    ) {
        if (adResourceIds.isNotEmpty()) {
            // Always show the complete advert. These source posters use a different aspect
            // ratio from the SUNMI display, so Crop was cutting off the artwork and forcing a
            // much larger upscale that made the advert look soft. Fit keeps the entire poster
            // visible, scales it only as much as necessary, and lets the black attract-mode
            // canvas absorb any unavoidable aspect-ratio difference without distortion.
            Image(
                painter = painterResource(adResourceIds[adIndex]),
                contentDescription = "Naomi-Chan promotion ${adIndex + 1} of ${adResourceIds.size}",
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PlaceholderPromotion()
        }

        // Lightweight scrims only where overlay text sits. The poster itself still fills
        // the complete screen underneath them.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(124.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))
                    )
                )
        )

        PlainHeader(
            time = time,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AdPositionDots(adResourceIds.size, adIndex)
            Spacer(modifier = Modifier.height(6.dp))
            NfcReaderPad()
        }
    }
}

@Composable
private fun PlainHeader(time: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Naomi-Chan™",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Official Network POS",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = time,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "ATTRACT MODE",
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
private fun AdPositionDots(count: Int, selected: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == selected) 7.dp else 5.dp)
                    .background(
                        color = if (index == selected) Color.White else Color.White.copy(alpha = 0.42f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun NfcReaderPad() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.Black.copy(alpha = 0.62f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Contactless,
                contentDescription = null,
                tint = Color(0xFF4B4F55),
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "NFC READY",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            )
            Text(
                text = "Tap anywhere to start",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Payment begins from Sell",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 8.sp
            )
        }

        Text(
            text = "HOLD\nNEAR TAG",
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PlaceholderPromotion() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = "Naomi-Chan™",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "BLACKPOOL DJ · ELECTRONIC PRODUCER",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
