#!/usr/bin/env python3
"""Naomi-Chan™ Blackpool Wireless POS Gateway.

The Flask gateway is the source of truth for the web/PDA catalogue.  Prices,
barcodes and item availability are validated server-side before wireless orders
are accepted.
"""

import json
import math
import os
import sqlite3
import time
import uuid
from typing import Any

from flask import Flask, jsonify, render_template, request

app = Flask(__name__, template_folder="templates", static_folder="static")
app.config["MAX_CONTENT_LENGTH"] = 128 * 1024

DB_PATH = os.path.join(os.path.dirname(__file__), "orders.db")
PAYMENT_METHODS = {"CARD", "CASH", "QR", "FREE", "WIRE"}
ITEM_TYPES = {"SERVICE", "ADMISSION", "MERCH", "TICKET", "VOUCHER"}

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

DEFAULT_CATALOG = [
    {"id": "item_shoutout", "name": "Live Track Shoutout", "price": 2.00, "category": "Microphone & DJ", "description": "Live dedication & shoutout over the club sound system", "barcode": "NCHN-SHOUTOUT", "item_type": "SERVICE"},
    {"id": "item_song_req", "name": "Guest Song Request", "price": 3.00, "category": "Microphone & DJ", "description": "Dedicated song played in next set rotation", "barcode": "NCHN-REQUEST", "item_type": "SERVICE"},
    {"id": "item_shot_voucher", "name": "Bar Shot / Drink Voucher", "price": 3.50, "category": "Bar & Drinks", "description": "Blackpool nightlife celebration voucher", "barcode": "NCHN-VOUCHER", "item_type": "VOUCHER"},
    {"id": "item_bar_admission", "name": "Standard Bar Admission", "price": 5.00, "category": "Admission & Door", "description": "Evening entry pass for a Naomi-Chan showcase", "barcode": "NCHN-ADMISSION", "item_type": "ADMISSION"},
    {"id": "item_lanyard", "name": "Naomi-Chan™ DJ Lanyard & Sticker", "price": 6.00, "category": "Merchandise", "description": "Official commemorative lanyard & vinyl sticker", "barcode": "NCHN-LANYARD", "item_type": "MERCH"},
    {"id": "item_vip_queue", "name": "VIP Fast-Track Wristband", "price": 8.00, "category": "VIP & Passes", "description": "Priority queue skip & event wristband", "barcode": "NCHN-VIP-FAST", "item_type": "TICKET"},
    {"id": "item_booth_token", "name": "VIP Booth Entry Token", "price": 10.00, "category": "VIP & Passes", "description": "Dedicated booth access token", "barcode": "NCHN-BOOTH", "item_type": "TICKET"},
    {"id": "item_stage_pass", "name": "DJ Stage Pass & Meet", "price": 12.00, "category": "VIP & Passes", "description": "Behind-the-decks access & meet pass", "barcode": "NCHN-STAGE", "item_type": "TICKET"},
    {"id": "item_all_night_vip", "name": "All-Night All-Access VIP Pass", "price": 15.00, "category": "VIP & Passes", "description": "Full night pass with priority access", "barcode": "NCHN-ALLNIGHT", "item_type": "TICKET"},
]


def get_db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, timeout=10.0)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA busy_timeout = 10000")
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def now_ms() -> int:
    return int(time.time() * 1000)


def clean_text(value: Any, default: str = "", max_length: int = 160) -> str:
    if value is None:
        return default
    text = str(value).strip()
    if not text:
        return default
    text = text.replace("<", "‹").replace(">", "›")
    text = "".join(ch for ch in text if ch >= " " or ch == "\t")
    return text[:max_length]


def json_object() -> dict[str, Any] | None:
    value = request.get_json(silent=True)
    return value if isinstance(value, dict) else None


def init_db() -> None:
    with get_db() as conn:
        conn.execute("PRAGMA journal_mode = WAL")
        conn.execute(
            """CREATE TABLE IF NOT EXISTS orders (
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
            )"""
        )
        conn.execute(
            """CREATE TABLE IF NOT EXISTS receipts (
                id TEXT PRIMARY KEY,
                receipt_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        conn.execute(
            """CREATE TABLE IF NOT EXISTS catalog_items (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                price REAL NOT NULL CHECK(price >= 0),
                category TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                barcode TEXT UNIQUE,
                item_type TEXT NOT NULL DEFAULT 'SERVICE',
                active INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )"""
        )
        stamp = now_ms()
        for item in DEFAULT_CATALOG:
            conn.execute(
                """INSERT OR IGNORE INTO catalog_items
                   (id, name, price, category, description, barcode, item_type, active, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)""",
                (item["id"], item["name"], item["price"], item["category"], item["description"], item["barcode"], item["item_type"], stamp, stamp),
            )
        conn.commit()


def catalog_to_dict(row: sqlite3.Row) -> dict[str, Any]:
    return {
        "id": row["id"],
        "name": row["name"],
        "price": round(float(row["price"]), 2),
        "category": row["category"],
        "description": row["description"],
        "desc": row["description"],
        "barcode": row["barcode"] or "",
        "item_type": row["item_type"],
        "active": bool(row["active"]),
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
    }


def get_catalog(include_inactive: bool = False) -> list[dict[str, Any]]:
    sql = "SELECT * FROM catalog_items"
    if not include_inactive:
        sql += " WHERE active = 1"
    sql += " ORDER BY category COLLATE NOCASE, name COLLATE NOCASE"
    with get_db() as conn:
        return [catalog_to_dict(row) for row in conn.execute(sql).fetchall()]


def find_catalog_item(*, item_id: str = "", name: str = "", barcode: str = "", active_only: bool = True) -> dict[str, Any] | None:
    clauses: list[str] = []
    params: list[Any] = []
    if item_id:
        clauses.append("id = ? COLLATE NOCASE")
        params.append(item_id)
    if name:
        clauses.append("name = ? COLLATE NOCASE")
        params.append(name)
    if barcode:
        clauses.append("barcode = ? COLLATE NOCASE")
        params.append(barcode)
    if not clauses:
        return None
    sql = "SELECT * FROM catalog_items WHERE (" + " OR ".join(clauses) + ")"
    if active_only:
        sql += " AND active = 1"
    sql += " LIMIT 1"
    with get_db() as conn:
        row = conn.execute(sql, params).fetchone()
    return catalog_to_dict(row) if row else None


def order_to_dict(row: sqlite3.Row) -> dict[str, Any]:
    try:
        items = json.loads(row["items_json"])
    except (TypeError, json.JSONDecodeError):
        items = []
    return {
        "id": row["id"], "client_name": row["client_name"], "client_contact": row["client_contact"],
        "venue_name": row["venue_name"], "items": items, "subtotal": row["subtotal"],
        "tax_percent": row["tax_percent"], "tax_amount": row["tax_amount"], "grand_total": row["grand_total"],
        "payment_method": row["payment_method"], "notes": row["notes"], "status": row["status"], "created_at": row["created_at"],
    }


def validate_catalog_payload(data: dict[str, Any], existing_id: str | None = None) -> tuple[dict[str, Any] | None, str | None]:
    name = clean_text(data.get("name"), max_length=120)
    category = clean_text(data.get("category"), "General", 80)
    description = clean_text(data.get("description", data.get("desc")), "", 400)
    barcode = clean_text(data.get("barcode"), "", 128)
    item_type = clean_text(data.get("item_type"), "SERVICE", 24).upper()
    item_id = clean_text(data.get("id"), existing_id or ("item_" + uuid.uuid4().hex[:10]), 64)
    try:
        price = round(float(data.get("price", 0)), 2)
    except (TypeError, ValueError):
        return None, "Price must be a number"
    if not name:
        return None, "Name is required"
    if not math.isfinite(price) or price < 0 or price > 100000:
        return None, "Price must be between £0.00 and £100,000.00"
    if item_type not in ITEM_TYPES:
        return None, "Unsupported item type"
    return {
        "id": existing_id or item_id,
        "name": name,
        "price": price,
        "category": category,
        "description": description,
        "barcode": barcode or None,
        "item_type": item_type,
        "active": 1 if bool(data.get("active", True)) else 0,
    }, None


init_db()


@app.after_request
def add_security_headers(response):
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["X-Frame-Options"] = "SAMEORIGIN"
    response.headers["Cache-Control"] = "no-store"
    return response


@app.errorhandler(413)
def payload_too_large(_error):
    return jsonify({"error": "Request payload is too large"}), 413


@app.route("/")
def index():
    return render_template("index.html", bars=BLACKPOOL_BARS)


@app.route("/catalog")
def catalog_page():
    return render_template("catalog.html")


@app.route("/api/status", methods=["GET"])
def api_status():
    with get_db() as conn:
        pending_count = conn.execute("SELECT COUNT(*) FROM orders WHERE status = 'PENDING'").fetchone()[0]
        total_count = conn.execute("SELECT COUNT(*) FROM orders").fetchone()[0]
        catalog_count = conn.execute("SELECT COUNT(*) FROM catalog_items WHERE active = 1").fetchone()[0]
        barcode_count = conn.execute("SELECT COUNT(*) FROM catalog_items WHERE active = 1 AND barcode IS NOT NULL AND barcode <> ''").fetchone()[0]
    return jsonify({
        "status": "ONLINE",
        "service": "Naomi-Chan™ Wireless POS Gateway",
        "currency": "GBP (£)",
        "default_vat": "20% UK VAT",
        "pending_orders": pending_count,
        "total_orders": total_count,
        "catalog_items": catalog_count,
        "barcode_items": barcode_count,
        "server_time": int(time.time()),
    })


@app.route("/api/bars", methods=["GET"])
def api_bars():
    return jsonify(BLACKPOOL_BARS)


@app.route("/api/items", methods=["GET"])
def api_items():
    return jsonify(get_catalog())


@app.route("/api/catalog", methods=["GET"])
def api_catalog():
    include_inactive = request.args.get("include_inactive", "0") in {"1", "true", "yes"}
    return jsonify(get_catalog(include_inactive=include_inactive))


@app.route("/api/catalog/barcode/<path:barcode>", methods=["GET"])
def api_catalog_barcode(barcode: str):
    safe_barcode = clean_text(barcode, max_length=128)
    item = find_catalog_item(barcode=safe_barcode)
    if item is None:
        return jsonify({"error": "Barcode not mapped", "barcode": safe_barcode}), 404
    return jsonify(item)


@app.route("/api/catalog", methods=["POST"])
def api_create_catalog_item():
    data = json_object()
    if data is None:
        return jsonify({"error": "Expected a JSON object"}), 400
    item, error = validate_catalog_payload(data)
    if error:
        return jsonify({"error": error}), 400
    assert item is not None
    stamp = now_ms()
    try:
        with get_db() as conn:
            conn.execute(
                """INSERT INTO catalog_items
                   (id, name, price, category, description, barcode, item_type, active, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (item["id"], item["name"], item["price"], item["category"], item["description"], item["barcode"], item["item_type"], item["active"], stamp, stamp),
            )
            conn.commit()
    except sqlite3.IntegrityError as exc:
        return jsonify({"error": "Item ID or barcode already exists", "detail": str(exc)}), 409
    return jsonify(find_catalog_item(item_id=item["id"], active_only=False)), 201


@app.route("/api/catalog/<item_id>", methods=["PUT"])
def api_update_catalog_item(item_id: str):
    safe_id = clean_text(item_id, max_length=64)
    data = json_object()
    if data is None:
        return jsonify({"error": "Expected a JSON object"}), 400
    item, error = validate_catalog_payload(data, existing_id=safe_id)
    if error:
        return jsonify({"error": error}), 400
    assert item is not None
    try:
        with get_db() as conn:
            cursor = conn.execute(
                """UPDATE catalog_items
                   SET name=?, price=?, category=?, description=?, barcode=?, item_type=?, active=?, updated_at=?
                   WHERE id=?""",
                (item["name"], item["price"], item["category"], item["description"], item["barcode"], item["item_type"], item["active"], now_ms(), safe_id),
            )
            conn.commit()
            if cursor.rowcount == 0:
                return jsonify({"error": "Catalog item not found"}), 404
    except sqlite3.IntegrityError as exc:
        return jsonify({"error": "Barcode already belongs to another item", "detail": str(exc)}), 409
    return jsonify(find_catalog_item(item_id=safe_id, active_only=False))


@app.route("/api/catalog/<item_id>", methods=["DELETE"])
def api_archive_catalog_item(item_id: str):
    safe_id = clean_text(item_id, max_length=64)
    with get_db() as conn:
        cursor = conn.execute("UPDATE catalog_items SET active=0, updated_at=? WHERE id=?", (now_ms(), safe_id))
        conn.commit()
    if cursor.rowcount == 0:
        return jsonify({"error": "Catalog item not found"}), 404
    return jsonify({"success": True, "id": safe_id, "active": False})


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
        rows = conn.execute("SELECT * FROM orders WHERE status='PENDING' ORDER BY created_at ASC LIMIT 200").fetchall()
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

    sanitized_items: list[dict[str, Any]] = []
    subtotal = 0.0
    for raw_item in raw_items:
        if not isinstance(raw_item, dict):
            return jsonify({"error": "Each line item must be an object"}), 400
        item_id = clean_text(raw_item.get("id"), max_length=64)
        name = clean_text(raw_item.get("name"), max_length=120)
        catalog_item = find_catalog_item(item_id=item_id, name=name)
        if catalog_item is None:
            return jsonify({"error": f"Unknown or inactive catalog item: {name or item_id or 'unnamed item'}"}), 400
        try:
            quantity = min(max(int(raw_item.get("quantity", 1)), 1), 99)
        except (TypeError, ValueError):
            return jsonify({"error": f"Invalid quantity for {catalog_item['name']}"}), 400
        unit_price = float(catalog_item["price"])
        subtotal += quantity * unit_price
        sanitized_items.append({"id": catalog_item["id"], "name": catalog_item["name"], "quantity": quantity, "unitPrice": unit_price})

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
    stamp = now_ms()
    with get_db() as conn:
        conn.execute(
            """INSERT INTO orders
               (id, client_name, client_contact, venue_name, items_json, subtotal, tax_percent, tax_amount, grand_total, payment_method, notes, status, created_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)""",
            (order_id, client_name, client_contact, venue_name, json.dumps(sanitized_items, separators=(",", ":")), subtotal, tax_percent, tax_amount, grand_total, payment_method, notes, stamp),
        )
        conn.commit()
    return jsonify({"success": True, "message": f"Wireless order {order_id} created", "order_id": order_id, "grand_total": grand_total, "status": "PENDING"}), 201


@app.route("/api/orders/<order_id>/printed", methods=["POST"])
def api_mark_printed(order_id: str):
    safe_id = clean_text(order_id, max_length=64)
    with get_db() as conn:
        cursor = conn.execute("UPDATE orders SET status='PRINTED' WHERE id=?", (safe_id,))
        conn.commit()
    if cursor.rowcount == 0:
        return jsonify({"error": "Order not found"}), 404
    return jsonify({"success": True, "message": f"Order {safe_id} marked as printed"})


@app.route("/api/receipts", methods=["POST"])
def api_upload_receipt():
    data = json_object()
    if data is None:
        return jsonify({"error": "Expected a JSON object"}), 400
    receipt_id = clean_text(data.get("id"), "NC-" + uuid.uuid4().hex[:8].upper(), 64)
    with get_db() as conn:
        conn.execute(
            """INSERT INTO receipts (id, receipt_json, created_at) VALUES (?, ?, ?)
               ON CONFLICT(id) DO UPDATE SET receipt_json=excluded.receipt_json, created_at=excluded.created_at""",
            (receipt_id, json.dumps(data, separators=(",", ":")), now_ms()),
        )
        conn.commit()
    return jsonify({"success": True, "id": receipt_id, "url": f"/receipt/{receipt_id}"})


@app.route("/api/receipts/<receipt_id>", methods=["GET"])
def api_get_receipt(receipt_id: str):
    safe_id = clean_text(receipt_id, max_length=64)
    with get_db() as conn:
        row = conn.execute("SELECT receipt_json FROM receipts WHERE id=?", (safe_id,)).fetchone()
    if row is None:
        return jsonify({"error": "Receipt not found"}), 404
    try:
        return jsonify(json.loads(row["receipt_json"]))
    except json.JSONDecodeError:
        return jsonify({"error": "Stored receipt is invalid"}), 500


@app.route("/receipt/<receipt_id>")
def receipt_page(receipt_id: str):
    safe_id = clean_text(receipt_id, max_length=64)
    with get_db() as conn:
        row = conn.execute("SELECT receipt_json FROM receipts WHERE id=?", (safe_id,)).fetchone()
    if row is None:
        return render_template("receipt.html", receipt=None, receipt_id=safe_id), 404
    try:
        receipt = json.loads(row["receipt_json"])
    except json.JSONDecodeError:
        return render_template("receipt.html", receipt=None, receipt_id=safe_id), 500
    return render_template("receipt.html", receipt=receipt, receipt_id=safe_id)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "5000")), debug=False)
