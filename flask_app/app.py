#!/usr/bin/env python3
"""Naomi-Chan™ Blackpool Wireless POS Gateway."""

import json
import math
import os
import sqlite3
import time
import uuid
from typing import Any

from flask import Flask, jsonify, render_template, request

app = Flask(__name__, template_folder="templates", static_folder="static")
app.config["MAX_CONTENT_LENGTH"] = 64 * 1024

DB_PATH = os.path.join(os.path.dirname(__file__), "orders.db")


def get_db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, timeout=10.0)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA busy_timeout = 10000")
    return conn


def init_db() -> None:
    with get_db() as conn:
        conn.execute("PRAGMA journal_mode = WAL")
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS orders (
                id TEXT PRIMARY KEY,
                client_name TEXT NOT NULL,
                client_contact TEXT,
                venue_name TEXT NOT NULL,
                items_json TEXT NOT NULL,
                subtotal REAL NOT NULL,
                tax_percent REAL NOT NULL DEFAULT 20.0,
                tax_amount REAL NOT NULL,
                grand_total REAL NOT NULL,
                payment_method TEXT NOT NULL,
                notes TEXT,
                status TEXT NOT NULL DEFAULT 'PENDING',
                created_at INTEGER NOT NULL
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS receipts (
                id TEXT PRIMARY KEY,
                receipt_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """
        )
        conn.commit()


init_db()

BLACKPOOL_BARS = [
    {"name": "The Flying Handbag", "address": "Queen St, FY1 2NL", "area": "Queen St & Gay Village", "contact": "01253 624519"},
    {"name": "Kaos Nightclub", "address": "38-42 Queen St, FY1 2NL", "area": "Queen St & Gay Village", "contact": "01253 751155"},
    {"name": "Funny Girls", "address": "5 Dickson Rd, FY1 2AX", "area": "Queen St & Gay Village", "contact": "01253 649194"},
    {"name": "Peek-a-Booze", "address": "8 Dickson Rd, FY1 2AX", "area": "Queen St & Gay Village", "contact": "01253 293585"},
    {"name": "Walkabout Blackpool", "address": "1-9 Queen St, FY1 1NL", "area": "Talbot Rd & Town Centre", "contact": "01253 749132"},
    {"name": "Popworld Blackpool", "address": "13 Promenade, FY1 1NL", "area": "Talbot Rd & Town Centre", "contact": "01253 294240"},
    {"name": "Ma Kelly's Central", "address": "Bank Hey St, FY1 4RU", "area": "Talbot Rd & Town Centre", "contact": "01253 623820"},
    {"name": "The Waterloo Music Bar", "address": "Waterloo Rd, FY4 2AF", "area": "South Shore & Waterloo", "contact": "01253 407886"},
    {"name": "North Pier Sunset Lounge", "address": "North Pier, Promenade, FY1 1NE", "area": "Promenade & Piers", "contact": "01253 623304"},
    {"name": "Central Pier Captain's Bar", "address": "Central Pier, Promenade, FY1 5BB", "area": "Promenade & Piers", "contact": "01253 622242"},
    {"name": "South Pier Beach Bar", "address": "South Pier, Promenade, FY4 1BB", "area": "Promenade & Piers", "contact": "01253 341030"},
    {"name": "Shenanigans Irish Bar", "address": "Talbot Rd, FY1 1LF", "area": "Talbot Rd & Town Centre", "contact": "01253 299119"},
    {"name": "Revolution Blackpool", "address": "Market St, FY1 1ET", "area": "Talbot Rd & Town Centre", "contact": "01253 299290"},
    {"name": "The Manchester Bar", "address": "Promenade, FY1 6BJ", "area": "South Shore & Waterloo", "contact": "01253 624519"},
]

MENU_ITEMS = [
    {"id": "item_shoutout", "name": "Live Track Shoutout", "price": 2.00, "category": "Microphone & DJ", "desc": "Live dedication & shoutout over the club sound system"},
    {"id": "item_song_req", "name": "Guest Song Request", "price": 3.00, "category": "Microphone & DJ", "desc": "Dedicated song played in next set rotation"},
    {"id": "item_shot_voucher", "name": "Bar Shot / Drink Voucher", "price": 3.50, "category": "Bar & Drinks", "desc": "Blackpool nightlife celebration shot token"},
    {"id": "item_bar_admission", "name": "Standard Bar Admission", "price": 5.00, "category": "Admission & Door", "desc": "Evening entry pass for Blackpool DJ showcase"},
    {"id": "item_lanyard", "name": "Naomi-Chan™ DJ Lanyard & Sticker", "price": 6.00, "category": "Merchandise", "desc": "Official commemorative lanyard & vinyl sticker"},
    {"id": "item_vip_queue", "name": "VIP Fast-Track Wristband", "price": 8.00, "category": "VIP & Passes", "desc": "Priority queue skip & glowing neon wristband"},
    {"id": "item_booth_token", "name": "VIP Booth Entry Token", "price": 10.00, "category": "VIP & Passes", "desc": "Dedicated booth seating access token"},
    {"id": "item_stage_pass", "name": "DJ Stage Pass & Meet", "price": 12.00, "category": "VIP & Passes", "desc": "Behind-the-decks access & meet Naomi-Chan"},
    {"id": "item_all_night_vip", "name": "All-Night All-Access VIP Pass", "price": 15.00, "category": "VIP & Passes", "desc": "Full night pass: priority entry, track priority & booth pass"},
]

MENU_BY_ID = {item["id"]: item for item in MENU_ITEMS}
MENU_BY_NAME = {item["name"]: item for item in MENU_ITEMS}
PAYMENT_METHODS = {"CARD", "CASH", "QR", "FREE", "WIRE"}


def clean_text(value: Any, default: str = "", max_length: int = 160) -> str:
    if value is None:
        return default
    text = str(value).strip()
    if not text:
        return default
    # Order rows are rendered by the bundled terminal with innerHTML. Neutralise tag
    # delimiters at ingestion so stored customer/venue text cannot become executable markup.
    text = text.replace("<", "‹").replace(">", "›")
    text = "".join(ch for ch in text if ch >= " " or ch == "\t")
    return text[:max_length]


def json_object() -> dict[str, Any] | None:
    data = request.get_json(silent=True)
    return data if isinstance(data, dict) else None


def order_to_dict(row: sqlite3.Row) -> dict[str, Any]:
    try:
        items = json.loads(row["items_json"])
    except (TypeError, json.JSONDecodeError):
        items = []
    return {
        "id": row["id"],
        "client_name": row["client_name"],
        "client_contact": row["client_contact"],
        "venue_name": row["venue_name"],
        "items": items,
        "subtotal": row["subtotal"],
        "tax_percent": row["tax_percent"],
        "tax_amount": row["tax_amount"],
        "grand_total": row["grand_total"],
        "payment_method": row["payment_method"],
        "notes": row["notes"],
        "status": row["status"],
        "created_at": row["created_at"],
    }


@app.after_request
def add_security_headers(response):
    # The bundled web terminal is same-origin and the Android client is not subject to browser CORS.
    # Deliberately do not emit Access-Control-Allow-Origin: * for order/receipt data.
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["Cache-Control"] = "no-store"
    return response


@app.errorhandler(413)
def payload_too_large(_error):
    return jsonify({"error": "Request payload is too large"}), 413


@app.route("/")
def index():
    return render_template("index.html", bars=BLACKPOOL_BARS, items=MENU_ITEMS)


@app.route("/api/status", methods=["GET"])
def api_status():
    with get_db() as conn:
        pending_count = conn.execute("SELECT COUNT(*) FROM orders WHERE status = 'PENDING'").fetchone()[0]
        total_count = conn.execute("SELECT COUNT(*) FROM orders").fetchone()[0]
    return jsonify({
        "status": "ONLINE",
        "service": "Naomi-Chan™ Blackpool Wireless POS Gateway",
        "currency": "GBP (£)",
        "price_range": "£2.00 - £15.00",
        "default_vat": "20% UK VAT",
        "pending_orders": pending_count,
        "total_orders": total_count,
        "server_time": int(time.time()),
    })


@app.route("/api/bars", methods=["GET"])
def api_bars():
    return jsonify(BLACKPOOL_BARS)


@app.route("/api/items", methods=["GET"])
def api_items():
    return jsonify(MENU_ITEMS)


@app.route("/api/orders", methods=["GET"])
def api_get_orders():
    requested_limit = request.args.get("limit", 50, type=int)
    limit = min(max(requested_limit or 50, 1), 200)
    with get_db() as conn:
        rows = conn.execute("SELECT * FROM orders ORDER BY created_at DESC LIMIT ?", (limit,)).fetchall()
    return jsonify([order_to_dict(row) for row in rows])


@app.route("/api/orders/pending", methods=["GET"])
def api_get_pending_orders():
    with get_db() as conn:
        rows = conn.execute(
            "SELECT * FROM orders WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 200"
        ).fetchall()
    return jsonify([order_to_dict(row) for row in rows])


@app.route("/api/orders", methods=["POST"])
def api_create_order():
    data = json_object()
    if data is None:
        return jsonify({"error": "Expected a JSON object"}), 400

    raw_items = data.get("items")
    if not isinstance(raw_items, list) or not raw_items:
        return jsonify({"error": "At least one valid catalog item is required"}), 400
    if len(raw_items) > 50:
        return jsonify({"error": "Too many line items"}), 400

    sanitized_items = []
    subtotal = 0.0
    for raw_item in raw_items:
        if not isinstance(raw_item, dict):
            return jsonify({"error": "Each line item must be an object"}), 400

        catalog_item = None
        item_id = clean_text(raw_item.get("id"), max_length=64)
        name = clean_text(raw_item.get("name"), max_length=120)
        if item_id:
            catalog_item = MENU_BY_ID.get(item_id)
        if catalog_item is None and name:
            catalog_item = MENU_BY_NAME.get(name)
        if catalog_item is None:
            return jsonify({"error": f"Unknown catalog item: {name or item_id or 'unnamed item'}"}), 400

        try:
            quantity = int(raw_item.get("quantity", 1))
        except (TypeError, ValueError):
            return jsonify({"error": f"Invalid quantity for {catalog_item['name']}"}), 400
        quantity = min(max(quantity, 1), 99)

        # Price always comes from the trusted server catalog. Ignore client-supplied unit_price.
        unit_price = float(catalog_item["price"])
        if not math.isfinite(unit_price):
            return jsonify({"error": "Catalog contains an invalid price"}), 500

        subtotal += quantity * unit_price
        sanitized_items.append({
            "id": catalog_item["id"],
            "name": catalog_item["name"],
            "quantity": quantity,
            "unitPrice": unit_price,
        })

    subtotal = round(subtotal, 2)
    tax_percent = 20.0
    tax_amount = round(subtotal * tax_percent / 100.0, 2)
    grand_total = round(subtotal + tax_amount, 2)

    payment_method = clean_text(data.get("payment_method"), "CARD", 16).upper()
    if payment_method not in PAYMENT_METHODS:
        return jsonify({"error": "Unsupported payment method"}), 400

    order_id = "WPOS-" + uuid.uuid4().hex[:8].upper()
    client_name = clean_text(data.get("client_name"), "Blackpool Guest", 120)
    client_contact = clean_text(data.get("client_contact"), "", 120)
    venue_name = clean_text(data.get("venue_name"), "The Flying Handbag, Queen St", 200)
    notes = clean_text(data.get("notes"), "Wireless order placed via web terminal", 500)
    now_ms = int(time.time() * 1000)

    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO orders (
                id, client_name, client_contact, venue_name, items_json,
                subtotal, tax_percent, tax_amount, grand_total, payment_method,
                notes, status, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
            """,
            (
                order_id,
                client_name,
                client_contact,
                venue_name,
                json.dumps(sanitized_items, separators=(",", ":")),
                subtotal,
                tax_percent,
                tax_amount,
                grand_total,
                payment_method,
                notes,
                now_ms,
            ),
        )
        conn.commit()

    return jsonify({
        "success": True,
        "message": f"Wireless order {order_id} created successfully for {venue_name}",
        "order_id": order_id,
        "grand_total": grand_total,
        "status": "PENDING",
    }), 201


@app.route("/api/orders/<order_id>/printed", methods=["POST"])
def api_mark_printed(order_id):
    safe_order_id = clean_text(order_id, max_length=64)
    with get_db() as conn:
        cursor = conn.execute("UPDATE orders SET status = 'PRINTED' WHERE id = ?", (safe_order_id,))
        conn.commit()
        if cursor.rowcount == 0:
            return jsonify({"error": "Order not found"}), 404
    return jsonify({"success": True, "message": f"Order {safe_order_id} marked as printed"})


@app.route("/api/receipts", methods=["POST"])
def api_upload_receipt():
    data = json_object()
    if data is None:
        return jsonify({"error": "Expected a JSON object"}), 400

    receipt_id = clean_text(data.get("id"), "NC-" + uuid.uuid4().hex[:8].upper(), 64)
    now_ms = int(time.time() * 1000)
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO receipts (id, receipt_json, created_at)
            VALUES (?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                receipt_json = excluded.receipt_json,
                created_at = excluded.created_at
            """,
            (receipt_id, json.dumps(data, separators=(",", ":")), now_ms),
        )
        conn.commit()
    return jsonify({"success": True, "receipt_id": receipt_id, "url": f"/receipt/{receipt_id}"})


@app.route("/receipt/<receipt_id>", methods=["GET"])
def view_receipt(receipt_id):
    safe_receipt_id = clean_text(receipt_id, max_length=64)
    with get_db() as conn:
        row = conn.execute("SELECT * FROM receipts WHERE id = ?", (safe_receipt_id,)).fetchone()
    if not row:
        return render_template("receipt.html", receipt=None, receipt_id=safe_receipt_id), 404
    try:
        receipt = json.loads(row["receipt_json"])
    except (TypeError, json.JSONDecodeError):
        return jsonify({"error": "Stored receipt is corrupted"}), 500
    return render_template("receipt.html", receipt=receipt)


if __name__ == "__main__":
    port = int(os.environ.get("FLASK_PORT", 5000))
    print(f"Starting Naomi-Chan Wireless POS Gateway on 0.0.0.0:{port}...")
    app.run(host="0.0.0.0", port=port, debug=False)
