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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GigType
import com.example.model.PackageTier
import com.example.model.PaymentMethod
import com.example.model.ReceiptData
import com.example.model.ReceiptItem
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
import java.util.Locale

private val wizardSteps = listOf(
    "Customer",
    "Event",
    "Services",
    "Payment",
    "Review"
)

@Composable
fun ReceiptWizardScreen(
    viewModel: PosViewModel,
    receipt: ReceiptData,
    step: Int,
    onStepChange: (Int) -> Unit,
    onNavigateToPreview: () -> Unit
) {
    val safeStep = step.coerceIn(0, wizardSteps.lastIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WizardHeader(
            receipt = receipt,
            step = safeStep,
            onReset = {
                viewModel.resetNewReceipt()
                onStepChange(0)
            }
        )

        WizardProgress(step = safeStep)

        when (safeStep) {
            0 -> CustomerStep(viewModel, receipt)
            1 -> EventStep(viewModel, receipt)
            2 -> ServicesStep(viewModel, receipt)
            3 -> PaymentStep(viewModel, receipt)
            else -> ReviewStep(
                viewModel = viewModel,
                receipt = receipt,
                onNavigateToPreview = onNavigateToPreview
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onStepChange((safeStep - 1).coerceAtLeast(0)) },
                enabled = safeStep > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            if (safeStep < wizardSteps.lastIndex) {
                Button(
                    onClick = { onStepChange((safeStep + 1).coerceAtMost(wizardSteps.lastIndex)) },
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next: ${wizardSteps[safeStep + 1]}")
                }
            } else {
                Button(
                    onClick = {
                        viewModel.resetNewReceipt()
                        onStepChange(0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiSurfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("New Receipt")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WizardHeader(receipt: ReceiptData, step: Int, onReset: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RECEIPT CREATOR",
                    color = NaomiOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = wizardSteps[step],
                    color = NaomiTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = receipt.id,
                    color = NaomiTextSecondary,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = onReset) {
                Text("Start Over", color = NaomiTextSecondary)
            }
        }
    }
}

@Composable
private fun WizardProgress(step: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        wizardSteps.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = when {
                                index < step -> NaomiSuccess
                                index == step -> NaomiRed
                                else -> NaomiSurfaceVariant
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < step) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = if (index == step) Color.White else NaomiTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = label,
                    color = if (index == step) NaomiOrange else NaomiTextSecondary,
                    fontSize = 9.sp,
                    fontWeight = if (index == step) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CustomerStep(viewModel: PosViewModel, receipt: ReceiptData) {
    WizardCard("1. Customer / Booking Contact", "Who is this receipt for?") {
        OutlinedTextField(
            value = receipt.clientName,
            onValueChange = viewModel::updateClientName,
            label = { Text("Customer or business name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = receipt.clientContact,
            onValueChange = viewModel::updateClientContact,
            label = { Text("Phone / contact") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EventStep(viewModel: PosViewModel, receipt: ReceiptData) {
    WizardCard("2. Event Details", "Set the venue, date and type of gig.") {
        OutlinedTextField(
            value = receipt.venueName,
            onValueChange = viewModel::updateVenue,
            label = { Text("Venue") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = receipt.gigDate,
            onValueChange = viewModel::updateGigDate,
            label = { Text("Event date / time") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Event type", color = NaomiTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        GigType.values().forEach { type ->
            FilterChip(
                selected = receipt.gigType == type,
                onClick = { viewModel.updateGigType(type) },
                label = { Text(type.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NaomiRed,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Free Event", color = NaomiTextPrimary, fontWeight = FontWeight.Bold)
                Text("Sets admission and VAT to £0.00", color = NaomiTextSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = receipt.isFreeEvent,
                onCheckedChange = viewModel::toggleFreeEvent
            )
        }
    }
}

@Composable
private fun ServicesStep(viewModel: PosViewModel, receipt: ReceiptData) {
    var itemName by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }

    WizardCard("3. Services & Items", "Choose a package and add anything extra.") {
        Text("Main package", color = NaomiTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        PackageTier.values().forEach { tier ->
            FilterChip(
                selected = receipt.packageTier == tier,
                onClick = { viewModel.updatePackageTier(tier) },
                label = {
                    Text(
                        if (tier.basePrice == 0.0) tier.title
                        else "${tier.title} · ${ReceiptData.formatCurrency(tier.basePrice)}"
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NaomiRed,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text("Receipt line items", color = NaomiTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (receipt.items.isEmpty()) {
            Text("No items yet.", color = NaomiTextSecondary, fontSize = 12.sp)
        }
        receipt.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NaomiDarkBg, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        "x${item.quantity} · ${ReceiptData.formatCurrency(item.total)}",
                        color = NaomiTextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = { viewModel.removeLineItem(item.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = NaomiOrange)
                }
            }
        }

        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Extra item / service") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = itemPrice,
            onValueChange = { itemPrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("Price (£)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val price = itemPrice.toDoubleOrNull()
                if (itemName.isNotBlank() && price != null && price >= 0.0) {
                    viewModel.addLineItem(ReceiptItem(name = itemName.trim(), unitPrice = price))
                    itemName = ""
                    itemPrice = ""
                }
            },
            enabled = itemName.isNotBlank() && itemPrice.toDoubleOrNull() != null,
            colors = ButtonDefaults.buttonColors(containerColor = NaomiSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Add Line Item")
        }
    }
}

@Composable
private fun PaymentStep(viewModel: PosViewModel, receipt: ReceiptData) {
    var taxText by remember(receipt.id) { mutableStateOf(receipt.taxPercent.toString()) }

    WizardCard("4. Payment & Notes", "Choose how it was paid and finish the receipt text.") {
        Text("Payment method", color = NaomiTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        PaymentMethod.values().forEach { method ->
            FilterChip(
                selected = receipt.paymentMethod == method,
                onClick = { viewModel.updatePaymentMethod(method) },
                label = { Text(method.label) },
                enabled = !receipt.isFreeEvent || method == PaymentMethod.FREE_PASS,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NaomiRed,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = if (receipt.isFreeEvent) "0" else taxText,
            onValueChange = { value ->
                val filtered = value.filter { it.isDigit() || it == '.' }
                taxText = filtered
                filtered.toDoubleOrNull()?.let(viewModel::updateTaxPercent)
            },
            enabled = !receipt.isFreeEvent,
            label = { Text("VAT / tax percent") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = receipt.footerNotes,
            onValueChange = viewModel::updateFooterNotes,
            label = { Text("Footer / thank-you note") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ReviewStep(
    viewModel: PosViewModel,
    receipt: ReceiptData,
    onNavigateToPreview: () -> Unit
) {
    WizardCard("5. Review & Print", "Confirm the totals before sending it to the printer.") {
        SummaryRow("Reference", receipt.id)
        SummaryRow("Customer", receipt.clientName)
        SummaryRow("Venue", receipt.venueName)
        SummaryRow("Event", receipt.gigType.label)
        SummaryRow("Payment", receipt.paymentMethod.label)
        SummaryRow("Subtotal", ReceiptData.formatCurrency(receipt.subtotal))
        SummaryRow("VAT (${receipt.taxPercent.toInt()}%)", ReceiptData.formatCurrency(receipt.taxAmount))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NaomiBorder)
        )

        SummaryRow(
            "TOTAL",
            if (receipt.isEffectivelyFree) "FREE" else ReceiptData.formatCurrency(receipt.grandTotal),
            emphasize = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToPreview,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Preview")
            }
            Button(
                onClick = { viewModel.printCurrentReceipt() },
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Print")
            }
        }

        Text(
            text = "Printing also saves the receipt locally and follows the existing gateway sync/offline rules.",
            color = NaomiTextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun WizardCard(
    title: String,
    subtitle: String,
    content: @Composable Column.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = NaomiTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 11.sp)
            content()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = if (emphasize) NaomiOrange else NaomiTextSecondary,
            fontWeight = if (emphasize) FontWeight.Black else FontWeight.Normal,
            fontSize = if (emphasize) 15.sp else 12.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            color = NaomiTextPrimary,
            fontWeight = if (emphasize) FontWeight.Black else FontWeight.Bold,
            fontSize = if (emphasize) 15.sp else 12.sp,
            modifier = Modifier.weight(0.6f)
        )
    }
}
