package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
private enum class ItemsMode { PACKAGE, EXTRAS }

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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        CompactSaleHeader(
            step = safeStep,
            receipt = receipt,
            onReset = {
                viewModel.resetNewReceipt()
                onStepChange(0)
            }
        )

        StepProgress(safeStep)

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            when (safeStep) {
                0 -> CustomerStep(viewModel, receipt)
                1 -> EventStep(viewModel, receipt)
                2 -> ItemsStep(viewModel, receipt)
                3 -> PaymentStep(viewModel, receipt)
                else -> ReviewStep(viewModel, receipt, onNavigateToPreview)
            }
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
    }
}

@Composable
private fun CompactSaleHeader(step: Int, receipt: ReceiptData, onReset: () -> Unit) {
    val itemCount = receipt.items.sumOf { it.quantity }
    val total = if (receipt.isEffectivelyFree) "FREE" else ReceiptData.formatCurrency(receipt.grandTotal)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("SELL · ${wizardSteps[step].uppercase()}", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
            Text(
                receipt.clientName.ifBlank { "New sale" },
                color = NaomiTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Step ${step + 1}/5 · $itemCount item(s) · ${receipt.id}",
                color = NaomiTextTertiary,
                fontSize = 7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(total, color = if (receipt.isEffectivelyFree) NaomiSuccess else NaomiTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
            TextButton(onClick = onReset, modifier = Modifier.height(28.dp)) {
                Text("Reset", color = NaomiTextSecondary, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun StepProgress(step: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        wizardSteps.forEachIndexed { index, label ->
            Surface(
                color = if (index == step) NaomiRed else if (index < step) NaomiOrange.copy(alpha = 0.18f) else NaomiSurface,
                border = BorderStroke(1.dp, if (index <= step) NaomiRed.copy(alpha = 0.45f) else NaomiBorder),
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.weight(1f).height(27.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (index == step) label else "${index + 1}",
                        color = if (index == step) Color.White else if (index < step) NaomiOrange else NaomiTextSecondary,
                        fontSize = if (index == step) 7.sp else 8.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerStep(viewModel: PosViewModel, receipt: ReceiptData) {
    StepPanel("Customer", "Who is this sale for?") {
        OutlinedTextField(
            value = receipt.clientName,
            onValueChange = viewModel::updateClientName,
            label = { Text("Customer or business name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(49.dp)
        )
        OutlinedTextField(
            value = receipt.clientContact,
            onValueChange = viewModel::updateClientContact,
            label = { Text("Phone / contact") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(49.dp)
        )
        Surface(
            color = NaomiSurfaceVariant,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NaomiBorder),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CUSTOMER RECORD", color = NaomiOrange, fontSize = 7.sp, fontWeight = FontWeight.Black)
                Box(modifier = Modifier.width(1.dp).height(26.dp).background(NaomiBorder))
                Text(
                    "Printed on receipt · searchable in Activity",
                    color = NaomiTextSecondary,
                    fontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EventStep(viewModel: PosViewModel, receipt: ReceiptData) {
    StepPanel("Event", "Venue, date and event type") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = receipt.venueName,
                onValueChange = viewModel::updateVenue,
                label = { Text("Venue") },
                singleLine = true,
                modifier = Modifier.weight(1f).height(53.dp)
            )
            OutlinedTextField(
                value = receipt.gigDate,
                onValueChange = viewModel::updateGigDate,
                label = { Text("Date / time") },
                singleLine = true,
                modifier = Modifier.weight(1f).height(53.dp)
            )
        }

        Text("EVENT TYPE", color = NaomiTextSecondary, fontSize = 7.sp, fontWeight = FontWeight.Black)
        GigType.values().toList().chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                pair.forEach { type ->
                    FilterChip(
                        selected = receipt.gigType == type,
                        onClick = { viewModel.updateGigType(type) },
                        label = { Text(type.label, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White),
                        modifier = Modifier.weight(1f).height(34.dp)
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        Surface(
            color = NaomiSurfaceVariant,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NaomiBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Free event", color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Text("Sets admission and VAT to £0.00", color = NaomiTextSecondary, fontSize = 7.sp)
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
    var mode by remember(receipt.id) { mutableStateOf(ItemsMode.PACKAGE) }
    var itemPage by remember(receipt.id) { mutableIntStateOf(0) }
    val itemPages = pageCount(receipt.items.size, 1)
    val visibleItem = pageSlice(receipt.items, itemPage, 1).firstOrNull()
    LaunchedEffect(receipt.items.size) { itemPage = itemPage.coerceIn(0, itemPages - 1) }

    StepPanel("Items", "Package and optional extras") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = mode == ItemsMode.PACKAGE,
                onClick = { mode = ItemsMode.PACKAGE },
                label = { Text("Package", fontSize = 8.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White),
                modifier = Modifier.weight(1f).height(34.dp)
            )
            FilterChip(
                selected = mode == ItemsMode.EXTRAS,
                onClick = { mode = ItemsMode.EXTRAS },
                label = { Text("Extras (${receipt.items.size})", fontSize = 8.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White),
                modifier = Modifier.weight(1f).height(34.dp)
            )
        }

        if (mode == ItemsMode.PACKAGE) {
            PackageTier.values().toList().chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    pair.forEach { tier ->
                        FilterChip(
                            selected = receipt.packageTier == tier,
                            onClick = { viewModel.updatePackageTier(tier) },
                            label = {
                                Text(
                                    if (tier.basePrice == 0.0) tier.title else "${tier.title} · ${ReceiptData.formatCurrency(tier.basePrice)}",
                                    fontSize = 7.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f).height(34.dp)
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) {
                    Text(receipt.packageTier.title, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    Text(receipt.packageTier.description, color = NaomiTextSecondary, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Surface(
                color = NaomiSurfaceVariant,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, NaomiBorder),
                modifier = Modifier.fillMaxWidth().height(57.dp)
            ) {
                if (visibleItem == null) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No extra items", color = NaomiTextSecondary, fontSize = 8.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(visibleItem.name, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("x${visibleItem.quantity} · ${ReceiptData.formatCurrency(visibleItem.total)}", color = NaomiTextSecondary, fontSize = 8.sp)
                        }
                        IconButton(onClick = { viewModel.removeLineItem(visibleItem.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove item", tint = NaomiRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            PosPager(
                page = itemPage,
                totalPages = itemPages,
                onPrevious = { itemPage = (itemPage - 1).coerceAtLeast(0) },
                onNext = { itemPage = (itemPage + 1).coerceAtMost(itemPages - 1) },
                label = "ITEM"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Extra item") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f).height(52.dp)
                )
                OutlinedTextField(
                    value = itemPrice,
                    onValueChange = { itemPrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("£") },
                    singleLine = true,
                    modifier = Modifier.weight(0.6f).height(52.dp)
                )
            }
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
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Add extra item", fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PaymentStep(viewModel: PosViewModel, receipt: ReceiptData) {
    var taxText by remember(receipt.id) { mutableStateOf(receipt.taxPercent.toString()) }

    StepPanel("Payment", "Record how the sale is settled") {
        Text("PAYMENT METHOD", color = NaomiTextSecondary, fontSize = 7.sp, fontWeight = FontWeight.Black)
        PaymentMethod.values().toList().chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                pair.forEach { method ->
                    FilterChip(
                        selected = receipt.paymentMethod == method,
                        onClick = { viewModel.updatePaymentMethod(method) },
                        label = { Text(method.label, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        enabled = !receipt.isFreeEvent || method == PaymentMethod.FREE_PASS,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White),
                        modifier = Modifier.weight(1f).height(34.dp)
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
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
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
        OutlinedTextField(
            value = receipt.footerNotes,
            onValueChange = viewModel::updateFooterNotes,
            label = { Text("Receipt note") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

@Composable
private fun ReviewStep(
    viewModel: PosViewModel,
    receipt: ReceiptData,
    onNavigateToPreview: () -> Unit
) {
    StepPanel("Review", "Confirm before completing") {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SummaryRow("Reference", receipt.id)
            SummaryRow("Customer", receipt.clientName.ifBlank { "—" })
            SummaryRow("Venue", receipt.venueName.ifBlank { "—" })
            SummaryRow("Event", receipt.gigType.label)
            SummaryRow("Payment", receipt.paymentMethod.label)
            SummaryRow("VAT", "${receipt.taxPercent.toInt()}% · ${ReceiptData.formatCurrency(receipt.taxAmount)}")
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NaomiBorder))
            SummaryRow("TOTAL", if (receipt.isEffectivelyFree) "FREE" else ReceiptData.formatCurrency(receipt.grandTotal), true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = onNavigateToPreview,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Preview", fontSize = 8.sp)
            }
            Button(
                onClick = viewModel::printCurrentReceipt,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1.4f).height(42.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Complete & print", fontWeight = FontWeight.Black, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun StepPanel(
    title: String,
    subtitle: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(title, color = NaomiTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 7.sp, maxLines = 1)
        }
        content()
    }
}

@Composable
private fun NavigationActions(
    step: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onNewSale: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = if (step == 0) onNewSale else onBack,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1f).height(42.dp)
        ) {
            if (step == 0) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("New", fontSize = 9.sp)
            } else {
                Text("‹ Back", fontSize = 9.sp)
            }
        }

        if (step < wizardSteps.lastIndex) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1.3f).height(38.dp)
            ) { Text("Continue ›", fontWeight = FontWeight.Black, fontSize = 9.sp) }
        } else {
            Button(
                onClick = onNewSale,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiSurfaceVariant),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1.3f).height(42.dp)
            ) { Text("New sale", color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp) }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (emphasize) NaomiTextPrimary else NaomiTextSecondary,
            fontWeight = if (emphasize) FontWeight.Black else FontWeight.Medium,
            fontSize = if (emphasize) 11.sp else 8.sp,
            modifier = Modifier.weight(0.36f)
        )
        Text(
            value,
            color = if (emphasize) NaomiRed else NaomiTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = if (emphasize) 13.sp else 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.64f)
        )
    }
}