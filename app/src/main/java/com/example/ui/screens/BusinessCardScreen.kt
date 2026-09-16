package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaymentMethod
import com.example.model.ReceiptData
import com.example.model.ReceiptItem
import com.example.printer.PrintResult
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import kotlinx.coroutines.launch

const val BUSINESS_CARD_PRINT_ID = "BUSINESS-CARD"

data class BusinessCardDraft(
    val businessName: String = "Naomi-Chan™",
    val displayName: String = "Naomi Extellna",
    val role: String = "Blackpool DJ & Electronic Producer",
    val email: String = "hello@naomi-chan.com",
    val phone: String = "",
    val website: String = "https://naomi-chan.com",
    val location: String = "Blackpool, United Kingdom"
)

@Composable
fun BusinessCardScreen(
    viewModel: PosViewModel,
    draft: BusinessCardDraft,
    onDraftChange: (BusinessCardDraft) -> Unit
) {
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val scope = rememberCoroutineScope()
    var printStatus by remember { mutableStateOf<String?>(null) }
    var printSucceeded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BUSINESS CARD",
                    color = NaomiOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "58mm Contact Card",
                    color = NaomiTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Create a compact branded card for the SUNMI V2 thermal printer.",
                    color = NaomiTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        BusinessCardPreview(draft)

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Card Details", color = NaomiTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)

                BusinessField("Business name", draft.businessName) { onDraftChange(draft.copy(businessName = it)) }
                BusinessField("Display name", draft.displayName) { onDraftChange(draft.copy(displayName = it)) }
                BusinessField("Role / title", draft.role) { onDraftChange(draft.copy(role = it)) }
                BusinessField("Email", draft.email) { onDraftChange(draft.copy(email = it)) }
                BusinessField("Phone", draft.phone) { onDraftChange(draft.copy(phone = it)) }
                BusinessField("Website / QR target", draft.website) { onDraftChange(draft.copy(website = it)) }
                BusinessField("Location", draft.location) { onDraftChange(draft.copy(location = it)) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDraftChange(BusinessCardDraft())
                            printStatus = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Reset")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                printStatus = "Sending business card to ${selectedChannel.displayName}..."
                                printSucceeded = false

                                val printReceipt = ReceiptData(
                                    id = BUSINESS_CARD_PRINT_ID,
                                    clientName = draft.businessName,
                                    clientContact = draft.displayName,
                                    venueName = draft.location,
                                    gigDate = draft.role,
                                    items = listOf(
                                        ReceiptItem(name = draft.email, quantity = 1, unitPrice = 0.0),
                                        ReceiptItem(name = draft.phone, quantity = 1, unitPrice = 0.0)
                                    ),
                                    paymentMethod = PaymentMethod.FREE_PASS,
                                    footerNotes = draft.website,
                                    taxPercent = 0.0,
                                    isFreeEvent = true
                                )

                                when (val result = viewModel.printerManager.printReceipt(printReceipt, selectedChannel, null)) {
                                    is PrintResult.Success -> {
                                        printSucceeded = true
                                        printStatus = "Business card sent to ${selectedChannel.displayName}."
                                    }
                                    is PrintResult.OutOfPaper -> {
                                        printStatus = result.message
                                    }
                                    is PrintResult.Error -> {
                                        printStatus = result.errorReason
                                    }
                                }
                            }
                        },
                        enabled = draft.businessName.isNotBlank() && draft.displayName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Print Card")
                    }
                }

                if (printStatus != null) {
                    Text(
                        text = printStatus.orEmpty(),
                        color = if (printSucceeded) NaomiSuccess else NaomiOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessCardPreview(draft: BusinessCardDraft) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = draft.businessName,
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = draft.displayName,
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = draft.role,
                color = Color.DarkGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(1.dp)
                    .background(Color.LightGray)
            )

            if (draft.email.isNotBlank()) Text(draft.email, color = Color.Black, fontSize = 11.sp)
            if (draft.phone.isNotBlank()) Text(draft.phone, color = Color.Black, fontSize = 11.sp)
            if (draft.website.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Web, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(draft.website, color = Color.Black, fontSize = 11.sp)
                }
            }
            if (draft.location.isNotBlank()) Text(draft.location, color = Color.DarkGray, fontSize = 10.sp)

            Spacer(modifier = Modifier.height(4.dp))
            Text("SCAN QR ON PRINTED CARD", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BusinessField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
