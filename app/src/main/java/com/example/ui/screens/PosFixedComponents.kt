package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

internal fun pageCount(itemCount: Int, pageSize: Int): Int =
    if (itemCount <= 0) 1 else ((itemCount - 1) / pageSize) + 1

internal fun <T> pageSlice(items: List<T>, page: Int, pageSize: Int): List<T> {
    if (items.isEmpty()) return emptyList()
    val safePage = page.coerceIn(0, pageCount(items.size, pageSize) - 1)
    val start = safePage * pageSize
    return items.subList(start, minOf(start + pageSize, items.size))
}

@Composable
internal fun PosPager(
    page: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "PAGE"
) {
    val safeTotal = totalPages.coerceAtLeast(1)
    val safePage = page.coerceIn(0, safeTotal - 1)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = safePage > 0,
            modifier = Modifier.weight(1f).height(42.dp),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NaomiBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiTextPrimary)
        ) {
            Text("‹ PREV", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Surface(
            modifier = Modifier.weight(1.1f).height(42.dp),
            color = NaomiSurface,
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$label ${safePage + 1}/$safeTotal",
                    color = NaomiTextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        OutlinedButton(
            onClick = onNext,
            enabled = safePage < safeTotal - 1,
            modifier = Modifier.weight(1f).height(42.dp),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NaomiBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiOrange)
        ) {
            Text("NEXT ›", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}