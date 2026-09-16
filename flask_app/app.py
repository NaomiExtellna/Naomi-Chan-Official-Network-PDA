#!/usr/bin/env python3
"""
Naomi-Chan™ DJ Services - Blackpool Wireless POS Web Gateway
Flask-based mobile web application for wireless order placement,
Blackpool bar venue selection, and real-time synchronization with
the Android 58mm thermal receipt printer.

All prices are strictly calibrated between £2.00 and £15.00.
"""

import json
import os
import sqlite3
import time
import uuid
from flask import Flask, request, jsonify, render_template, send_from_directory

app = Flask(__name__, template_folder="templates", static_folder="static")

# Database initialization
DB_PATH = os.path.join(os.path.dirname(__file__), "orders.db")

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    with get_db() as conn:
        conn.execute("""
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
        """)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS receipts (
                id TEXT PRIMARY KEY,
                receipt_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)
        conn.commit()

init_db()

# Blackpool Bars & Venues Catalog
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
    {"name": "The Manchester Bar", "address": "Promenade, FY1 6BJ", "area": "South Shore & Waterloo", "contact": "01253 624519"}
]

# Menu Items - Strictly between £2.00 and £15.00
MENU_ITEMS = [
    {
        "id": "item_shoutout",
        "name": "Live Track Shoutout",
        "price": 2.00,
        "category": "Microphone & DJ",
        "desc": "Live dedication & shoutout over the club sound system"
    },
    {
        "id": "item_song_req",
        "name": "Guest Song Request",
        "price": 3.00,
        "category": "Microphone & DJ",
        "desc": "Dedicated song played in next set rotation"
    },
    {
        "id": "item_shot_voucher",
        "name": "Bar Shot / Drink Voucher",
        "price": 3.50,
        "category": "Bar & Drinks",
        "desc": "Blackpool nightlife celebration shot token"
    },
    {
        "id": "item_bar_admission",
        "name": "Standard Bar Admission",
        "price": 5.00,
        "category": "Admission & Door",
        "desc": "Evening entry pass for Blackpool DJ showcase"
    },
    {
        "id": "item_lanyard",
        "name": "Naomi-Chan™ DJ Lanyard & Sticker",
        "price": 6.00,
        "category": "Merchandise",
        "desc": "Official commemorative lanyard & vinyl sticker"
    },
    {
        "id": "item_vip_queue",
        "name": "VIP Fast-Track Wristband",
        "price": 8.00,
        "category": "VIP & Passes",
        "desc": "Priority queue skip & glowing neon wristband"
    },
    {
        "id": "item_booth_token",
        "name": "VIP Booth Entry Token",
        "price": 10.00,
        "category": "VIP & Passes",
        "desc": "Dedicated booth seating access token"
    },
    {
        "id": "item_stage_pass",
        "name": "DJ Stage Pass & Meet",
        "price": 12.00,
        "category": "VIP & Passes",
        "desc": "Behind-the-decks access & meet Naomi-Chan"
    },
    {
        "id": "item_all_night_vip",
        "name": "All-Night All-Access VIP Pass",
        "price": 15.00,
        "category": "VIP & Passes",
        "desc": "Full night pass: priority entry, track priority & booth pass"
    }
]

# CORS support for wireless mobile devices
@app.after_request
def add_cors_headers(response):
    response.headers["Access-Control-Allow-Origin"] = "*"
    response.headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS"
    response.headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization"
    return response

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
        "server_time": int(time.time())
    })

@app.route("/api/bars", methods=["GET"])
def api_bars():
    return jsonify(BLACKPOOL_BARS)

@app.route("/api/items", methods=["GET"])
def api_items():
    return jsonify(MENU_ITEMS)

@app.route("/api/orders", methods=["GET"])
def api_get_orders():
    limit = request.args.get("limit", 50, type=int)
    with get_db() as conn:
        rows = conn.execute(
            "SELECT * FROM orders ORDER BY created_at DESC LIMIT ?", (limit,)
        ).fetchall()
        orders = []
        for r in rows:
            orders.append({
                "id": r["id"],
                "client_name": r["client_name"],
                "client_contact": r["client_contact"],
                "venue_name": r["venue_name"],
                "items": json.loads(r["items_json"]),
                "subtotal": r["subtotal"],
                "tax_percent": r["tax_percent"],
                "tax_amount": r["tax_amount"],
                "grand_total": r["grand_total"],
                "payment_method": r["payment_method"],
                "notes": r["notes"],
                "status": r["status"],
                "created_at": r["created_at"]
            })
    return jsonify(orders)

@app.route("/api/orders/pending", methods=["GET"])
def api_get_pending_orders():
    with get_db() as conn:
        rows = conn.execute(
            "SELECT * FROM orders WHERE status = 'PENDING' ORDER BY created_at ASC"
        ).fetchall()
        orders = []
        for r in rows:
            orders.append({
                "id": r["id"],
                "client_name": r["client_name"],
                "client_contact": r["client_contact"],
                "venue_name": r["venue_name"],
                "items": json.loads(r["items_json"]),
                "subtotal": r["subtotal"],
                "tax_percent": r["tax_percent"],
                "tax_amount": r["tax_amount"],
                "grand_total": r["grand_total"],
                "payment_method": r["payment_method"],
                "notes": r["notes"],
                "status": r["status"],
                "created_at": r["created_at"]
            })
    return jsonify(orders)

@app.route("/api/orders", methods=["POST"])
def api_create_order():
    data = request.get_json(force=True)
    order_id = "WPOS-" + str(uuid.uuid4())[:8].upper()
    client_name = data.get("client_name", "Blackpool Guest").strip() or "Blackpool Guest"
    client_contact = data.get("client_contact", "").strip()
    venue_name = data.get("venue_name", "The Flying Handbag, Queen St").strip()
    items = data.get("items", [])
    payment_method = data.get("payment_method", "CARD")
    notes = data.get("notes", "Wireless order placed via web terminal").strip()

    # Validate items and calculate subtotal
    subtotal = 0.0
    sanitized_items = []
    for it in items:
        name = it.get("name", "Item")
        qty = max(1, int(it.get("quantity", 1)))
        price = float(it.get("unit_price", 0.0))
        # Ensure price is clamped between £2.00 and £15.00 if non-zero
        if price > 0.0:
            price = max(2.00, min(15.00, price))
        subtotal += (qty * price)
        sanitized_items.append({
            "name": name,
            "quantity": qty,
            "unitPrice": price
        })

    tax_percent = 20.0
    tax_amount = round(subtotal * (tax_percent / 100.0), 2)
    grand_total = round(subtotal + tax_amount, 2)
    now_ms = int(time.time() * 1000)

    with get_db() as conn:
        conn.execute("""
            INSERT INTO orders (
                id, client_name, client_contact, venue_name, items_json,
                subtotal, tax_percent, tax_amount, grand_total, payment_method,
                notes, status, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
        """, (
            order_id, client_name, client_contact, venue_name,
            json.dumps(sanitized_items), subtotal, tax_percent,
            tax_amount, grand_total, payment_method, notes, now_ms
        ))
        conn.commit()

    return jsonify({
        "success": True,
        "message": f"Wireless order {order_id} created successfully for {venue_name}",
        "order_id": order_id,
        "grand_total": grand_total,
        "status": "PENDING"
    }), 201

@app.route("/api/orders/<order_id>/printed", methods=["POST"])
def api_mark_printed(order_id):
    with get_db() as conn:
        cursor = conn.execute("UPDATE orders SET status = 'PRINTED' WHERE id = ?", (order_id,))
        conn.commit()
        if cursor.rowcount == 0:
            return jsonify({"error": "Order not found"}), 404
    return jsonify({"success": True, "message": f"Order {order_id} marked as printed"})

@app.route("/api/receipts", methods=["POST"])
def api_upload_receipt():
    data = request.get_json(force=True)
    receipt_id = data.get("id", "NC-" + str(uuid.uuid4())[:6].upper())
    now_ms = int(time.time() * 1000)
    with get_db() as conn:
        conn.execute("""
            INSERT OR REPLACE INTO receipts (id, receipt_json, created_at)
            VALUES (?, ?, ?)
        """, (receipt_id, json.dumps(data), now_ms))
        conn.commit()
    return jsonify({"success": True, "receipt_id": receipt_id, "url": f"/receipt/{receipt_id}"})

@app.route("/receipt/<receipt_id>", methods=["GET"])
def view_receipt(receipt_id):
    with get_db() as conn:
        row = conn.execute("SELECT * FROM receipts WHERE id = ?", (receipt_id,)).fetchone()
        if not row:
            return f"<h3>Receipt {receipt_id} not found on wireless server.</h3>", 404
        receipt = json.loads(row["receipt_json"])
    return render_template("receipt.html", receipt=receipt)

if __name__ == "__main__":
    port = int(os.environ.get("FLASK_PORT", 5000))
    print(f"Starting Naomi-Chan Wireless POS Gateway on 0.0.0.0:{port}...")
    app.run(host="0.0.0.0", port=port, debug=False)
