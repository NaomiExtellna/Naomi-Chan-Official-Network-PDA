package com.naomichan.pos.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val Blue=Color(0xFF2457A6)
private val Navy=Color(0xFF15243A)
private val Surface=Color(0xFFF6F8FB)

private val Scheme=lightColorScheme(
  primary=Blue,
  onPrimary=Color.White,
  primaryContainer=Color(0xFFE8F0FF),
  onPrimaryContainer=Navy,
  background=Surface,
  surface=Color.White,
  onSurface=Navy,
  outline=Color(0xFFD9E0EA)
)

@Composable
fun NaomiPosTheme(content:@Composable()->Unit)=MaterialTheme(
  colorScheme=Scheme,
  typography=Typography().run{
    copy(
      titleLarge=titleLarge.copy(fontWeight=FontWeight.Bold),
      titleMedium=titleMedium.copy(fontWeight=FontWeight.SemiBold),
      labelLarge=labelLarge.copy(fontWeight=FontWeight.SemiBold)
    )
  },
  content=content
)
