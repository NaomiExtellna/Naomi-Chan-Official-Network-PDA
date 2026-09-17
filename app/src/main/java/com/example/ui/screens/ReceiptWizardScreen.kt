package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.NaomiTextTertiary

private val wizardSteps = listOf("Customer", "Event", "Items", "Payment", "Review")

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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SaleHeader(
            step = safeStep,
            receipt = receipt,
            onReset = {
                viewModel.resetNewReceipt()
                onStepChange(0)
            }
        )

        SaleSummaryCard(receipt)
        StepProgress(safeStep)

        when (safeStep) {
            0 -> CustomerStep(viewModel, receipt)
            1 -> EventStep(viewModel, receipt)
            2 -> ItemsStep(viewModel, receipt)
            3 -> PaymentStep(viewModel, receipt)
            else -> ReviewStep(viewModel, receipt, onNavigateToPreview)
        }

        NavigationActions(
            step = safeStep,
            onBack = { onStepChange((safeStep - 1).coerceAtLeast(0)) },
            onNext = { onStepChange((safeStep + 1).coerceAtMost(wizardSteps.lastIndex)) },
            onNewSale = {
                viewModel.resetNewReceipt()
                onStepChange(0)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SaleHeader(step: Int, receipt: ReceiptData, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sell",
                color = NaomiTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Step ${step + 1} of ${wizardSteps.size} · ${wizardSteps[step]}",
                color = NaomiTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = receipt.id,
                color = NaomiTextTertiary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onReset) {
            Text("Start over", color = NaomiTextSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SaleSummaryCard(receipt: ReceiptData) {
    val itemCount = receipt.items.sumOf { it.quantity }
    val total = if (receipt.isEffectivelyFree) "FREE" else ReceiptData.formatCurrency(receipt.grandTotal)

    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Current sale", color = NaomiTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        receipt.clientName.ifBlank { "No customer yet" },
                        color = NaomiTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        receipt.venueName.ifBlank { "No venue selected" },
                        color = NaomiTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(total, color = NaomiTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (itemCount == 1) "1 item" else "$itemCount items",
                        color = NaomiTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StepProgress(step: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            wizardSteps.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(
                            color = if (index <= step) NaomiRed else NaomiBorder,
                            shape = RoundedCornerShape(5.dp)
                        )
                )
            }
        }
        Text(
            text = wizardSteps.joinToString("  ·  "),
            color = NaomiTextTertiary,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun CustomerStep(viewModel: PosViewModel, receipt: ReceiptData) {
    PosCard("Customer", "Who is this sale for?") {
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
    PosCard("Event", "Set the venue, date and type of booking.") {
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

        SectionLabel("Event type")
        GigType.values().forEach { type ->
            FilterChip(
                selected = receipt.gigType == type,
                onClick = { viewModel.updateGigType(type) },
                label = { Text(type.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NaomiRed,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }

        Surface(
            color = NaomiSurfaceVariant,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NaomiBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Free event", color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Sets admission and VAT to £0.00", color = NaomiTextSecondary, fontSize = 11.sp)
                }
                Switch(checked = receipt.isFreeEvent, onCheckedChange = viewModel::toggleFreeEvent)
            }
        }
    }
}

@Composable
private fun ItemsStep(viewModel: PosViewModel, receipt: ReceiptData) {
    var itemName by remember(receipt.id) { mutableStateOf("") }
    var itemPrice by remember(receipt.id) { mutableStateOf("") }

    PosCard("Items", "Choose a package and add any extras.") {
        SectionLabel("Package")
        PackageTier.values().forEach { tier ->
            FilterChip(
                selected = receipt.packageTier == tier,
                onClick = { viewModel.updatePackageTier(tier) },
                label = {
                    Text(
                        if (tier.basePrice == 0.0) tier.title
                        else "${tier.title} · ${ReceiptData.formatCurrency(tier.basePrice)}",
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NaomiRed,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }

        SectionLabel("Extra items")
        if (receipt.items.isEmpty()) {
            EmptyInlineState(
                title = "No extra items added",
                subtitle = "Add optional services or merchandise below."
            )
        } else {
            receipt.items.forEach { item ->
                Surface(
                    color = NaomiSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NaomiBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "x${item.quantity} · ${ReceiptData.formatCurrency(item.total)}",
                                color = NaomiTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { viewModel.removeLineItem(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove item", tint = NaomiRed)
                        }
                    }
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
        OutlinedButton(
            onClick = {
                val price = itemPrice.toDoubleOrNull()
                if (itemName.isNotBlank() && price != null && price >= 0.0) {
                    viewModel.addLineItem(ReceiptItem(name = itemName.trim(), unitPrice = price))
                    itemName = ""
                    itemPrice = ""
                }
            },
            enabled = itemName.isNotBlank() && itemPrice.toDoubleOrNull() != null,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(7.dp))
            Text("Add item", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PaymentStep(viewModel: PosViewModel, receipt: ReceiptData) {
    var taxText by remember(receipt.id) { mutableStateOf(receipt.taxPercent.toString()) }

    PosCard("Payment", "Record how this sale is being settled.") {
        SectionLabel("Payment method")
        PaymentMethod.values().forEach { method ->
            FilterChip(
                selected = receipt.paymentMethod == method,
                onClick = { viewModel.updatePaymentMethod(method) },
                label = { Text(method.label, fontSize = 12.sp) },
                enabled = !receipt.isFreeEvent || method == PaymentMethod.FREE_PASS,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NaomiRed,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }

        OutlinedTextField(
            value = if (receipt.isFreeEvent) "0" else taxText,
            onValueChange = { value ->
                taxText = value.filter { it.isDigit() || it == '.' }
                taxText.toDoubleOrNull()?.let(viewModel::updateTaxPercent)
            },
            enabled = !receipt.isFreeEvent,
            label = { Text("VAT / tax percent") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = receipt.footerNotes,
            onValueChange = viewModel::updateFooterNotes,
            label = { Text("Receipt note") },
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
    PosCard("Review", "Confirm the details before completing the sale.") {
        SummaryRow("Reference", receipt.id)
        SummaryRow("Customer", receipt.clientName.ifBlank { "—" })
        SummaryRow("Venue", receipt.venueName.ifBlank { "—" })
        SummaryRow("Event", receipt.gigType.label)
        SummaryRow("Payment", receipt.paymentMethod.label)
        SummaryRow("Subtotal", ReceiptData.formatCurrency(receipt.subtotal))
        SummaryRow("VAT (${receipt.taxPercent.toInt()}%)", ReceiptData.formatCurrency(receipt.taxAmount))

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NaomiBorder))
        SummaryRow(
            label = "Total",
            value = if (receipt.isEffectivelyFree) "FREE" else ReceiptData.formatCurrency(receipt.grandTotal),
            emphasize = true
        )

        OutlinedButton(
            onClick = onNavigateToPreview,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(7.dp))
            Text("Preview receipt", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = viewModel::printCurrentReceipt,
            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Complete sale & print", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }

        Text(
            "Completing the sale saves it locally and follows the existing gateway sync/offline rules.",
            color = NaomiTextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun NavigationActions(
    step: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onNewSale: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = if (step == 0) onNewSale else onBack,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(54.dp)
        ) {
            if (step == 0) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("New sale")
            } else {
                Text("Back")
            }
        }

        if (step < wizardSteps.lastIndex) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(54.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.Black)
            }
        } else {
            Button(
                onClick = onNewSale,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(54.dp)
            ) {
                Text("New sale", color = NaomiTextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PosCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NaomiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = NaomiTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 12.sp)
            content()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = NaomiTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EmptyInlineState(title: String, subtitle: String) {
    Surface(
        color = NaomiSurfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NaomiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 11.sp)
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
            label,
            color = if (emphasize) NaomiTextPrimary else NaomiTextSecondary,
            fontWeight = if (emphasize) FontWeight.Black else FontWeight.Medium,
            fontSize = if (emphasize) 16.sp else 12.sp,
            modifier = Modifier.weight(0.40f)
        )
        Text(
            value,
            color = if (emphasize) NaomiRed else NaomiTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = if (emphasize) 18.sp else 12.sp,
            modifier = Modifier.weight(0.60f)
        )
    }
}
