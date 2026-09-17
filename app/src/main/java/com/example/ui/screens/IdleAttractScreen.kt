package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.ui.draw.clip
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
private const val CLOCK_TICK_MS = 30_000L

@Composable
fun IdleAttractScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity) {
        val window = activity?.window
        val decorView = window?.decorView
        val previousSystemUi = decorView?.systemUiVisibility ?: 0
        val previousStatusBarColor = window?.statusBarColor
        val previousNavigationBarColor = window?.navigationBarColor

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

        onDispose {
            if (decorView != null) decorView.systemUiVisibility = previousSystemUi
            if (previousStatusBarColor != null) window?.statusBarColor = previousStatusBarColor
            if (previousNavigationBarColor != null) window?.navigationBarColor = previousNavigationBarColor
        }
    }

    val adResourceIds = remember(context) {
        val bundled = listOf(
            R.drawable.promo_ad_01,
            R.drawable.promo_ad_02,
            R.drawable.promo_ad_03,
            R.drawable.promo_ad_04
        )
        val optional = (5..6).mapNotNull { index ->
            context.resources.getIdentifier(
                "promo_ad_${index.toString().padStart(2, '0')}",
                "drawable",
                context.packageName
            ).takeIf { it != 0 }
        }
        bundled + optional
    }

    var adIndex by remember(adResourceIds) { mutableIntStateOf(0) }
    var clockTick by remember { mutableIntStateOf(0) }
    val tapInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(adResourceIds.size) {
        if (adResourceIds.size > 1) {
            while (true) {
                delay(AD_ROTATION_MS)
                adIndex = (adIndex + 1) % adResourceIds.size
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            clockTick = (System.currentTimeMillis() / CLOCK_TICK_MS).toInt()
            delay(CLOCK_TICK_MS)
        }
    }

    val time = remember(clockTick) {
        SimpleDateFormat("HH:mm", Locale.UK).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
            .clickable(
                interactionSource = tapInteraction,
                indication = null,
                onClick = onDismiss
            )
    ) {
        if (adResourceIds.isNotEmpty()) {
            // Decorative edge-to-edge backdrop. Cropping is intentional here only; it is
            // dimmed so the readable campaign artwork below remains the focal point.
            Image(
                painter = painterResource(adResourceIds[adIndex]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.42f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.48f)
                            )
                        )
                    )
            )

            // The actual advert is never stretched or cropped. ContentScale.Fit preserves
            // the PNG's original poster ratio and uses the dimmed backdrop as letterboxing.
            val posterShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = 82.dp, bottom = 132.dp)
                    .clip(posterShape)
                    .background(Color(0x8F10161D))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)), posterShape)
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(adResourceIds[adIndex]),
                    contentDescription = "Naomi-Chan promotion",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            PlaceholderPromotion()
        }

        GlassHeader(
            time = time,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (adResourceIds.size > 1) {
                AdPositionGlassDots(adResourceIds.size, adIndex)
            }
            ContactlessGlassPrompt()
        }
    }
}

@Composable
private fun GlassHeader(time: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color(0xC5161C23))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), shape)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.img_naomi_logo),
                contentDescription = "Naomi-Chan logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Column {
                Text("Naomi-Chan™", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("Official Network POS", color = Color.White.copy(alpha = 0.68f), fontSize = 8.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("ATTRACT MODE", color = Color.White.copy(alpha = 0.56f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdPositionGlassDots(count: Int, selected: Int) {
    val shape = RoundedCornerShape(99.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color(0xA010161D))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == selected) 8.dp else 6.dp)
                    .background(
                        if (index == selected) Color.White else Color.White.copy(alpha = 0.34f),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ContactlessGlassPrompt() {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xD9161C23))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), shape)
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Contactless,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "READY WHEN YOU ARE",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color(0xFFFF9A6F),
                    modifier = Modifier.size(17.dp)
                )
                Text("Tap anywhere to start", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Text(
                "Display only · Payment begins from Sell",
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 7.sp
            )
        }
    }
}

@Composable
private fun PlaceholderPromotion() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFC84532), Color(0xFFE7834F), Color(0xFF6F2A34))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Image(
                painter = painterResource(R.drawable.img_naomi_logo),
                contentDescription = null,
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
            Text("Naomi-Chan™", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(
                "BLACKPOOL DJ · ELECTRONIC PRODUCER",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 10.sp,
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