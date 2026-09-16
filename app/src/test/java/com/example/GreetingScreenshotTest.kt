package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.ReceiptData
import com.example.model.ReceiptItem
import com.example.ui.components.ThermalReceiptPaper
import com.example.ui.theme.NaomiChanTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun thermalReceipt_screenshot() {
        val sampleReceipt = ReceiptData(
            id = "NC-998811",
            clientName = "Elena Rostova",
            clientContact = "+49 170 998822",
            venueName = "Berghain Berlin",
            gigDate = "Saturday • 23:00 - 05:00",
            items = listOf(
                ReceiptItem(name = "Main Stage Resident DJ Set (6h)", quantity = 1, unitPrice = 850.0),
                ReceiptItem(name = "Atmospheric Fog Rig", quantity = 1, unitPrice = 65.0),
                ReceiptItem(name = "Naomi-Chan™ Vinyl LP", quantity = 2, unitPrice = 30.0)
            )
        )

        composeTestRule.setContent {
            NaomiChanTheme(darkTheme = true) {
                ThermalReceiptPaper(receipt = sampleReceipt)
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/receipt_paper.png")
    }
}
