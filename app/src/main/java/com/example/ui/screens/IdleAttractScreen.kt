package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Customer-facing idle display for the SUNMI V2.
 *
 * This deliberately looks like a modern contactless terminal without pretending to
 * process NFC payments. Any tap exits attract mode and returns to the real POS flow.
 *
 * Drop campaign PNGs into res/drawable using these names and they are discovered at runtime:
 * promo_ad_01.png ... promo_ad_06.png
 */
@Composable
fun IdleAttractScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val adResourceIds = remember(context) {
        (1..6).mapNotNull { index ->
            val name = "promo_ad_${index.toString().padStart(2, '0')}"
            context.resources
                .getIdentifier(name, "drawable", context.packageName)
                .takeIf { it != 0 }
        }
    }

    var adIndex by remember(adResourceIds) { mutableIntStateOf(0) }
    var clockText by remember { mutableIntStateOf(0) }
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
            clockText = (System.currentTimeMillis() / 30_000L).toInt()
            delay(30_000L)
        }
    }

    val time = remember(clockText) {
        SimpleDateFormat("HH:mm", Locale.UK).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF07111A),
                        Color(0xFF0A2231),
                        Color(0xFF0B1822)
                    )
                )
            )
            .clickable(
                interactionSource = tapInteraction,
                indication = null,
                onClick = onDismiss
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = Color.White.copy(alpha = 0.10f)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_naomi_logo),
                            contentDescription = "Naomi-Chan logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Naomi-Chan™",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Official Network POS",
                            color = Color.White.copy(alpha = 0.64f),
                            fontSize = 9.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = time,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "IDLE DISPLAY",
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.07f)
            ) {
                if (adResourceIds.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(adResourceIds[adIndex]),
                            contentDescription = "Naomi-Chan promotion",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                    )
                                )
                        )
                    }
                } else {
                    PlaceholderPromotion()
                }
            }

            if (adResourceIds.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    adResourceIds.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (index == adIndex) 8.dp else 6.dp)
                                .background(
                                    if (index == adIndex) Color.White else Color.White.copy(alpha = 0.30f),
                                    CircleShape
                                )
                        )
                    }
                }
            }

            ContactlessPrompt()
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
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.img_naomi_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(22.dp)),
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

        Text(
            text = "PROMOTIONAL DISPLAY",
            color = Color.White.copy(alpha = 0.66f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun ContactlessPrompt() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Contactless,
                contentDescription = null,
                tint = Color(0xFF111820),
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "READY WHEN YOU ARE",
                color = Color(0xFF111820),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color(0xFFC84532),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Tap screen to start",
                    color = Color(0xFF111820),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "Display mode only · Payment starts from the Sell flow",
                color = Color(0xFF68727D),
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
