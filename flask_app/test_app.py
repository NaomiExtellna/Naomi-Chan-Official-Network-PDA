import os
import tempfile
import unittest

import app as gateway


class GatewayApiTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.original_db_path = gateway.DB_PATH
        gateway.DB_PATH = os.path.join(self.temp_dir.name, "test_orders.db")
        gateway.init_db()
        gateway.app.config.update(TESTING=True)
        self.client = gateway.app.test_client()

    def tearDown(self):
        gateway.DB_PATH = self.original_db_path
        self.temp_dir.cleanup()

    def test_status_endpoint_is_available(self):
        response = self.client.get("/api/status")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json()["status"], "ONLINE")
        self.assertNotIn("Access-Control-Allow-Origin", response.headers)

    def test_order_uses_server_catalog_price_not_client_price(self):
        response = self.client.post(
            "/api/orders",
            json={
                "client_name": "Test Guest",
                "venue_name": "Test Venue",
                "payment_method": "CARD",
                "items": [
                    {
                        "name": "Live Track Shoutout",
                        "quantity": 2,
                        "unit_price": 0.01,
                    }
                ],
            },
        )
        self.assertEqual(response.status_code, 201)
        body = response.get_json()
        self.assertAlmostEqual(body["grand_total"], 4.80, places=2)

        orders = self.client.get("/api/orders").get_json()
        self.assertEqual(len(orders), 1)
        self.assertAlmostEqual(orders[0]["subtotal"], 4.00, places=2)
        self.assertAlmostEqual(orders[0]["items"][0]["unitPrice"], 2.00, places=2)

    def test_unknown_catalog_item_is_rejected(self):
        response = self.client.post(
            "/api/orders",
            json={
                "items": [{"name": "Injected Item", "quantity": 1, "unit_price": 0.01}]
            },
        )
        self.assertEqual(response.status_code, 400)

    def test_invalid_payment_method_is_rejected(self):
        response = self.client.post(
            "/api/orders",
            json={
                "payment_method": "NOT_REAL",
                "items": [{"name": "Live Track Shoutout", "quantity": 1}],
            },
        )
        self.assertEqual(response.status_code, 400)

    def test_missing_order_is_not_marked_printed(self):
        response = self.client.post("/api/orders/WPOS-NOTFOUND/printed")
        self.assertEqual(response.status_code, 404)

    def test_receipt_round_trip(self):
        payload = {
            "id": "NC-TEST1234",
            "clientName": "Test Client",
            "venueName": "Test Venue",
            "gigDate": "2026-09-16 21:00",
            "items": [],
            "subtotal": 0.0,
            "taxPercent": 0.0,
            "taxAmount": 0.0,
            "grandTotal": 0.0,
            "paymentMethod": "Free Pass / Comp",
            "footerNotes": "Test receipt",
        }
        upload = self.client.post("/api/receipts", json=payload)
        self.assertEqual(upload.status_code, 200)
        receipt_page = self.client.get("/receipt/NC-TEST1234")
        self.assertEqual(receipt_page.status_code, 200)
        self.assertIn(b"Test Client", receipt_page.data)

    def test_missing_receipt_returns_safe_404_page(self):
        response = self.client.get("/receipt/NC-NOTFOUND")
        self.assertEqual(response.status_code, 404)
        self.assertIn(b"RECEIPT NOT FOUND", response.data)


if __name__ == "__main__":
    unittest.main()
