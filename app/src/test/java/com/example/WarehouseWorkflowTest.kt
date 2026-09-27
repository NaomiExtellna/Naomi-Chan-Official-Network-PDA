package com.example

import com.example.ui.WarehouseManifestLine
import com.example.ui.WarehouseOrder
import com.example.ui.WarehouseStage
import com.example.ui.WarehouseStockItem
import com.example.ui.WarehouseUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarehouseWorkflowTest {

    @Test
    fun manifest_is_complete_only_when_every_line_is_picked() {
        val incomplete = WarehouseOrder(
            reference = "NCHN-TEST-BKPL",
            recipient = "Test",
            postcode = "FY1",
            createdLabel = "Now",
            stage = WarehouseStage.PROCESSING,
            manifest = listOf(
                WarehouseManifestLine(
                    id = "1",
                    sku = "SKU-1",
                    name = "One",
                    location = "A-01",
                    quantity = 1,
                    picked = true
                ),
                WarehouseManifestLine(
                    id = "2",
                    sku = "SKU-2",
                    name = "Two",
                    location = "A-02",
                    quantity = 1,
                    picked = false
                )
            )
        )

        assertFalse(incomplete.allPicked)
        assertTrue(
            incomplete.copy(
                manifest = incomplete.manifest.map { it.copy(picked = true) }
            ).allPicked
        )
    }

    @Test
    fun low_stock_uses_the_configured_threshold_inclusively() {
        assertTrue(
            WarehouseStockItem("A", "Item", "M", "A-01", 4, 4).isLowStock
        )
        assertFalse(
            WarehouseStockItem("A", "Item", "M", "A-01", 5, 4).isLowStock
        )
    }

    @Test
    fun command_counts_follow_bfc_operational_states() {
        val processing = warehouseOrder("ONE", WarehouseStage.PROCESSING)
        val packed = warehouseOrder("TWO", WarehouseStage.PACKED)
        val transit = warehouseOrder("THREE", WarehouseStage.IN_TRANSIT)
        val delivered = warehouseOrder("FOUR", WarehouseStage.DELIVERED)
        val held = warehouseOrder("FIVE", WarehouseStage.PROCESSING).copy(
            holdType = "Quality control"
        )

        val state = WarehouseUiState(
            orders = listOf(processing, packed, transit, delivered, held),
            stock = listOf(
                WarehouseStockItem("LOW", "Low", "Standard", "B-01", 2, 3),
                WarehouseStockItem("OK", "OK", "Standard", "B-02", 10, 3)
            )
        )

        assertEquals(3, state.pickPackCount)
        assertEquals(1, state.dispatchCount)
        assertEquals(1, state.lowStockCount)
        assertEquals(1, state.openHoldCount)
    }

    private fun warehouseOrder(
        reference: String,
        stage: WarehouseStage
    ) = WarehouseOrder(
        reference = reference,
        recipient = "Test",
        postcode = "FY1",
        createdLabel = "Now",
        stage = stage,
        manifest = listOf(
            WarehouseManifestLine(
                id = reference,
                sku = reference,
                name = reference,
                location = "A-01",
                quantity = 1
            )
        )
    )
}
