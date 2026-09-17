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

    // The SUNMI V2 runs Android 7.1.x, so use the legacy immersive flags rather than
    // relying on modern edge-to-edge APIs. The previous system UI state is restored as
    // soon as attract mode is dismissed.
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
            if (decorView != null) {
                decorView.systemUiVisibility = previousSystemUi
            }
            if (previousStatusBarColor != null) {
                window?.statusBarColor = previousStatusBarColor
            }
            if (previousNavigationBarColor != null) {
                window?.navigationBarColor = previousNavigationBarColor
            }
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
            val name = "promo_ad_${index.toString().padStart(2, '0')}"
            context.resources
                .getIdentifier(name, "drawable", context.packageName)
                .takeIf { it != 0 }
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
            Image(
                painter = painterResource(adResourceIds[adIndex]),
                contentDescription = "Naomi-Chan promotion",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PlaceholderPromotion()
        }

        // Preserve artwork impact while ensuring the glass controls remain readable over
        // both bright and dark campaign images.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.32f),
                        0.24f to Color.Transparent,
                        0.58f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.60f)
                    )
                )
        )

        GlassHeader(
            time = time,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (adResourceIds.size > 1) {
                AdPositionGlassDots(
                    count = adResourceIds.size,
                    selected = adIndex
                )
            }
            ContactlessGlassPrompt()
        }
    }
}

@Composable
private fun GlassHeader(
    time: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xD91A2028),
                        Color(0xA611171E)
                    )
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.img_naomi_logo),
                    contentDescription = "Naomi-Chan logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                            RoundedCornerShape(11.dp)
                        )
                )
                Column {
                    Text(
                        text = "Naomi-Chan™",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Official Network POS",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = time,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "ATTRACT MODE",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun AdPositionGlassDots(
    count: Int,
    selected: Int
) {
    val shape = RoundedCornerShape(99.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color(0x8A10161D))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == selected) 8.dp else 6.dp)
                    .background(
                        if (index == selected) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.35f)
                        },
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ContactlessGlassPrompt() {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xE11B222B),
                        Color(0xC710161D)
                    )
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)), shape)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(Color.White.copy(alpha = 0.10f), CircleShape)
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "READY WHEN YOU ARE",
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.0.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color(0xFFFF9A6F),
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = "Tap anywhere to start",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = "Display mode only · Payment begins from Sell",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
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
                    listOf(
                        Color(0xFFC84532),
                        Color(0xFFE7834F),
                        Color(0xFF6F2A34)
                    )
                )
            )
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.img_naomi_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "Naomi-Chan™",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "BLACKPOOL DJ · ELECTRONIC PRODUCER",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Live events · VRChat · Private bookings",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
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
