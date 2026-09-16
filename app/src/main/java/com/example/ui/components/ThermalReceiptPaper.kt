package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ReceiptData
import com.example.model.ReceiptIconType
import com.example.ui.theme.NaomiPaperInk
import com.example.ui.theme.NaomiPaperWhite

@Composable
fun ThermalReceiptPaper(
    receipt: ReceiptData,
    customIconBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(2.dp))
                .background(NaomiPaperWhite)
                .testTag("thermal_receipt_paper")
        ) {
            // Jagged top tear edge
            ThermalTearEdge(isTop = true)

            // Receipt Content
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Icon at Top (Rendered based on selected iconType)
                if (receipt.iconType != ReceiptIconType.NONE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (receipt.iconType == ReceiptIconType.CUSTOM && customIconBitmap != null) {
                            Image(
                                bitmap = customIconBitmap.asImageBitmap(),
                                contentDescription = "Custom Receipt Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .heightIn(max = 70.dp)
                                    .widthIn(max = 220.dp)
                                    .padding(horizontal = 4.dp)
                            )
                        } else {
                            val resId = receipt.iconType.drawableResId ?: R.drawable.img_naomi_logo
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, NaomiPaperInk, CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = receipt.iconType.label,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Brand Header
                Text(
                    text = "NAOMI-CHAN™",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp,
                    color = NaomiPaperInk,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PREMIUM DJ SERVICES",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NaomiPaperInk,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Blackpool • North West UK • Golden Mile",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NaomiPaperInk.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "www.naomichan-dj.com",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NaomiPaperInk.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                // Prominent Free Admission Banner if free event
                if (receipt.isEffectivelyFree) {
                    ThermalDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F8E9))
                            .border(1.dp, NaomiPaperInk, RoundedCornerShape(2.dp))
                            .padding(vertical = 5.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "*** FREE ADMISSION ***",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = NaomiPaperInk,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "COMPLIMENTARY PASS / NO COVER",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = NaomiPaperInk,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                ThermalDivider()

                // 2. Booking Metadata
                ThermalTwoColumnRow(label = "RECEIPT NO:", value = receipt.id, isBold = true)
                ThermalTwoColumnRow(label = "DATE/TIME:", value = receipt.formattedDate())
                ThermalTwoColumnRow(label = "GIG TYPE:", value = receipt.gigType.label)
                ThermalTwoColumnRow(label = "VENUE:", value = receipt.venueName)
                ThermalTwoColumnRow(label = "CLIENT:", value = receipt.clientName)
                ThermalTwoColumnRow(label = "CONTACT:", value = receipt.clientContact)

                ThermalDivider()

                // 3. Itemized List Header
                Text(
                    text = "--- SERVICES & MERCHANDISE ---",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = NaomiPaperInk,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ITEM",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NaomiPaperInk
                    )
                    Text(
                        text = "AMOUNT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NaomiPaperInk
                    )
                }

                ThermalDottedDivider()

                // Line items
                for (item in receipt.items) {
                    val itemTitle = if (item.quantity > 1) "${item.name} x${item.quantity}" else item.name
                    val priceStr = if (item.unitPrice == 0.0) "FREE" else ReceiptData.formatCurrency(item.total)
                    ThermalTwoColumnRow(
                        label = itemTitle,
                        value = priceStr
                    )
                }

                ThermalDivider()

                // 4. Financial breakdown
                if (receipt.isEffectivelyFree) {
                    ThermalTwoColumnRow(label = "ADMISSION PASS:", value = "FREE (${ReceiptData.formatCurrency(0.0)})", isBold = true)
                    ThermalTwoColumnRow(label = "SUBTOTAL:", value = ReceiptData.formatCurrency(0.0))
                    ThermalTwoColumnRow(label = "VAT (0%):", value = ReceiptData.formatCurrency(0.0))
                    ThermalDoubleDivider()
                    ThermalTwoColumnRow(
                        label = "TOTAL DUE:",
                        value = "${ReceiptData.formatCurrency(0.0)} (FREE)",
                        isBig = true,
                        isBold = true
                    )
                    ThermalDoubleDivider()
                    ThermalTwoColumnRow(label = "PAYMENT METHOD:", value = "FREE PASS / COMP", isBold = true)
                    ThermalTwoColumnRow(
                        label = "STATUS:",
                        value = if (receipt.isBufferedOffline) "BUFFERED (OFFLINE)" else "ADMITTED & VERIFIED",
                        isBold = true
                    )
                } else {
                    ThermalTwoColumnRow(label = "SUBTOTAL:", value = ReceiptData.formatCurrency(receipt.subtotal))
                    if (receipt.discountPercent > 0) {
                        ThermalTwoColumnRow(
                            label = "DISCOUNT (${receipt.discountPercent.toInt()}%):",
                            value = "-${ReceiptData.formatCurrency(receipt.discountAmount)}"
                        )
                    }
                    ThermalTwoColumnRow(label = "VAT (${receipt.taxPercent.toInt()}%):", value = ReceiptData.formatCurrency(receipt.taxAmount))
                    ThermalDoubleDivider()

                    // Grand Total
                    ThermalTwoColumnRow(
                        label = "TOTAL DUE:",
                        value = ReceiptData.formatCurrency(receipt.grandTotal),
                        isBig = true,
                        isBold = true
                    )
                    ThermalDoubleDivider()
                    ThermalTwoColumnRow(label = "PAYMENT METHOD:", value = receipt.paymentMethod.label)
                    ThermalTwoColumnRow(
                        label = "STATUS:",
                        value = if (receipt.isBufferedOffline) "BUFFERED (OFFLINE)" else "VERIFIED & PAID",
                        isBold = true
                    )
                }

                ThermalDivider()

                // Footer Notes
                if (receipt.footerNotes.isNotEmpty()) {
                    Text(
                        text = receipt.footerNotes,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NaomiPaperInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Simulated ESC/POS QR Code & Barcode box
                ThermalQrBox(bookingId = receipt.id)

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Booking Ref: ${receipt.id}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = NaomiPaperInk
                )
                Text(
                    text = "Naomi-Chan™ DJ Sound Collective",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = NaomiPaperInk.copy(alpha = 0.7f)
                )
                Text(
                    text = "Thank you for rocking with us!",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = NaomiPaperInk.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "---------------- [ CUT HERE ] ----------------",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = NaomiPaperInk.copy(alpha = 0.4f)
                )
            }

            // Jagged bottom tear edge
            ThermalTearEdge(isTop = false)
        }
    }
}

@Composable
fun ThermalTwoColumnRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isBig: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBig) 13.sp else 10.5.sp,
            color = NaomiPaperInk,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBig) 14.sp else 10.5.sp,
            color = NaomiPaperInk,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ThermalDivider() {
    Text(
        text = "--------------------------------",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = NaomiPaperInk.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    )
}

@Composable
fun ThermalDottedDivider() {
    Text(
        text = "................................",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = NaomiPaperInk.copy(alpha = 0.4f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

@Composable
fun ThermalDoubleDivider() {
    Text(
        text = "================================",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        color = NaomiPaperInk,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

@Composable
fun ThermalTearEdge(isTop: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val toothWidth = 14f
        val toothHeight = size.height
        val teethCount = (size.width / toothWidth).toInt() + 1
        val path = Path()

        if (isTop) {
            path.moveTo(0f, toothHeight)
            for (i in 0..teethCount) {
                val x = i * toothWidth
                val y = if (i % 2 == 0) 0f else toothHeight
                path.lineTo(x, y)
            }
            path.lineTo(size.width, toothHeight)
            path.close()
        } else {
            path.moveTo(0f, 0f)
            for (i in 0..teethCount) {
                val x = i * toothWidth
                val y = if (i % 2 == 0) toothHeight else 0f
                path.lineTo(x, y)
            }
            path.lineTo(size.width, 0f)
            path.close()
        }

        drawPath(
            path = path,
            color = Color(0xFF101014) // Cuts into canvas background
        )
    }
}

@Composable
fun ThermalQrBox(bookingId: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        // High contrast thermal QR pattern representation
        Canvas(modifier = Modifier.size(80.dp)) {
            val step = size.width / 9f
            drawRect(color = NaomiPaperInk, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width, size.height), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

            // Corner eyes
            drawRect(color = NaomiPaperInk, topLeft = Offset(6f, 6f), size = androidx.compose.ui.geometry.Size(step * 2.5f, step * 2.5f))
            drawRect(color = NaomiPaperWhite, topLeft = Offset(11f, 11f), size = androidx.compose.ui.geometry.Size(step * 1.5f, step * 1.5f))
            drawRect(color = NaomiPaperInk, topLeft = Offset(15f, 15f), size = androidx.compose.ui.geometry.Size(step * 0.7f, step * 0.7f))

            drawRect(color = NaomiPaperInk, topLeft = Offset(size.width - step * 2.5f - 6f, 6f), size = androidx.compose.ui.geometry.Size(step * 2.5f, step * 2.5f))
            drawRect(color = NaomiPaperWhite, topLeft = Offset(size.width - step * 2.5f - 1f, 11f), size = androidx.compose.ui.geometry.Size(step * 1.5f, step * 1.5f))

            drawRect(color = NaomiPaperInk, topLeft = Offset(6f, size.height - step * 2.5f - 6f), size = androidx.compose.ui.geometry.Size(step * 2.5f, step * 2.5f))

            // Center data dots
            for (i in 3..6) {
                for (j in 3..6) {
                    if ((i + j) % 2 == 0) {
                        drawRect(
                            color = NaomiPaperInk,
                            topLeft = Offset(i * step, j * step),
                            size = androidx.compose.ui.geometry.Size(step * 0.8f, step * 0.8f)
                        )
                    }
                }
            }
        }
        Text(
            text = "[ SCAN TO VERIFY GIG BOOKING ]",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = NaomiPaperInk,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
