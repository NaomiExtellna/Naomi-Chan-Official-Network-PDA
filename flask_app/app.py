#!/usr/bin/env python3
"""Naomi-Chan™ DJ Company event operations and wireless POS gateway.

The Flask gateway is the source of truth for events, catalogue items, barcode
mapping, web orders, connected PDA terminals and receipt verification.
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
app.config["SEND_FILE_MAX_AGE_DEFAULT"] = 3600

DB_PATH = os.path.join(os.path.dirname(__file__), "orders.db")
PAYMENT_METHODS = {"CARD", "CASH", "QR", "FREE", "WIRE"}
ITEM_TYPES = {"SERVICE", "EVENT_ADDON", "ADMISSION", "TICKET", "VIP", "MERCH", "VOUCHER"}
EVENT_STATUSES = {"DRAFT", "LIVE", "CLOSED"}
DEVICE_ONLINE_MS = 120_000

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
    {"id": "item_shoutout", "name": "Live Track Shoutout", "price": 2.00, "category": "DJ Add-ons", "description": "Live dedication and shoutout during the event", "barcode": "NCHN-SHOUTOUT", "item_type": "EVENT_ADDON"},
    {"id": "item_song_req", "name": "Guest Song Request", "price": 3.00, "category": "DJ Add-ons", "description": "Priority guest song request for the current event", "barcode": "NCHN-REQUEST", "item_type": "EVENT_ADDON"},
    {"id": "item_bar_admission", "name": "Standard Event Admission", "price": 5.00, "category": "Admission & Door", "description": "Standard admission to a Naomi-Chan DJ Company event", "barcode": "NCHN-ADMISSION", "item_type": "ADMISSION"},
    {"id": "item_lanyard", "name": "Naomi-Chan™ DJ Lanyard & Sticker", "price": 6.00, "category": "Merchandise", "description": "Official event lanyard and vinyl sticker", "barcode": "NCHN-LANYARD", "item_type": "MERCH"},
    {"id": "item_vip_queue", "name": "VIP Fast-Track Wristband", "price": 8.00, "category": "VIP & Passes", "description": "Priority entry and event wristband upgrade", "barcode": "NCHN-VIP-FAST", "item_type": "VIP"},
    {"id": "item_booth_token", "name": "VIP Booth Entry Token", "price": 10.00, "category": "VIP & Passes", "description": "Dedicated VIP booth access token", "barcode": "NCHN-BOOTH", "item_type": "VIP"},
    {"id": "item_stage_pass", "name": "DJ Stage Pass & Meet", "price": 12.00, "category": "VIP & Passes", "description": "Behind-the-decks access and meet pass", "barcode": "NCHN-STAGE", "item_type": "VIP"},
    {"id": "item_all_night_vip", "name": "All-Night All-Access VIP Pass", "price": 15.00, "category": "VIP & Passes", "description": "Full-event access with priority entry and VIP areas", "barcode": "NCHN-ALLNIGHT", "item_type": "VIP"},
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


def ensure_column(conn: sqlite3.Connection, table: str, column: str, declaration: str) -> None:
    columns = {row[1] for row in conn.execute(f"PRAGMA table_info({table})").fetchall()}
    if column not in columns:
        conn.execute(f"ALTER TABLE {table} ADD COLUMN {column} {declaration}")


def init_db() -> None:
    with get_db() as conn:
        conn.execute("PRAGMA journal_mode = WAL")
        conn.execute(
            """CREATE TABLE IF NOT EXISTS events (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                venue_name TEXT NOT NULL DEFAULT '',
                starts_at TEXT NOT NULL DEFAULT '',
                ends_at TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'DRAFT',
                notes TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )"""
        )
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
        conn.execute(
            """CREATE TABLE IF NOT EXISTS devices (
                id TEXT PRIMARY KEY,
                device_name TEXT NOT NULL,
                manufacturer TEXT NOT NULL DEFAULT '',
                model TEXT NOT NULL DEFAULT '',
                android_version TEXT NOT NULL DEFAULT '',
                sdk INTEGER NOT NULL DEFAULT 0,
                app_version TEXT NOT NULL DEFAULT '',
                operator_name TEXT NOT NULL DEFAULT '',
                shift_id TEXT NOT NULL DEFAULT '',
                printer_connected INTEGER NOT NULL DEFAULT 0,
                unsynced_receipts INTEGER NOT NULL DEFAULT 0,
                remote_addr TEXT NOT NULL DEFAULT '',
                first_seen INTEGER NOT NULL,
                last_seen INTEGER NOT NULL
            )"""
        )

        ensure_column(conn, "orders", "event_id", "TEXT")
        ensure_column(conn, "orders", "target_device_id", "TEXT")
        ensure_column(conn, "catalog_items", "event_id", "TEXT")

        conn.executescript(
            """
            CREATE INDEX IF NOT EXISTS index_orders_status_created_at
                ON orders(status, created_at);
            CREATE INDEX IF NOT EXISTS index_orders_event_created_at
                ON orders(event_id, created_at);
            CREATE INDEX IF NOT EXISTS index_orders_target_status_created_at
                ON orders(target_device_id, status, created_at);
            CREATE INDEX IF NOT EXISTS index_orders_created_at
                ON orders(created_at);
            CREATE INDEX IF NOT EXISTS index_events_status_starts_at
                ON events(status, starts_at);
            CREATE INDEX IF NOT EXISTS index_catalog_active_event
                ON catalog_items(active, event_id);
            CREATE INDEX IF NOT EXISTS index_receipts_created_at
                ON receipts(created_at);
            CREATE INDEX IF NOT EXISTS index_devices_last_seen
                ON devices(last_seen);
            """
        )

        stamp = now_ms()
        for item in DEFAULT_CATALOG:
            conn.execute(
                """INSERT OR IGNORE INTO catalog_items
                   (id, name, price, category, description, barcode, item_type, active, created_at, updated_at, event_id)
                   VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, NULL)""",
                (item["id"], item["name"], item["price"], item["category"], item["description"], item["barcode"], item["item_type"], stamp, stamp),
            )
        conn.commit()


def event_to_dict(row: sqlite3.Row) -> dict[str, Any]:
    return {
        "id": row["id"],
        "name": row["name"],
        "venue_name": row["venue_name"],
        "starts_at": row["starts_at"],
        "ends_at": row["ends_at"],
        "status": row["status"],
        "notes": row["notes"],
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
    }


def get_events(include_closed: bool = False) -> list[dict[str, Any]]:
    sql = "SELECT * FROM events"
    if not include_closed:
        sql += " WHERE status <> 'CLOSED'"
    sql += " ORDER BY CASE status WHEN 'LIVE' THEN 0 WHEN 'DRAFT' THEN 1 ELSE 2 END, starts_at, name COLLATE NOCASE"
    with get_db() as conn:
        return [event_to_dict(row) for row in conn.execute(sql).fetchall()]


def find_event(event_id: str, conn: sqlite3.Connection | None = None) -> dict[str, Any] | None:
    if not event_id:
        return None
    if conn is not None:
        row = conn.execute("SELECT * FROM events WHERE id=? LIMIT 1", (event_id,)).fetchone()
    else:
        with get_db() as database:
            row = database.execute("SELECT * FROM events WHERE id=? LIMIT 1", (event_id,)).fetchone()
    return event_to_dict(row) if row else None


def validate_event_payload(data: dict[str, Any], existing_id: str | None = None) -> tuple[dict[str, Any] | None, str | None]:
    name = clean_text(data.get("name"), max_length=140)
    venue_name = clean_text(data.get("venue_name"), "", 200)
    starts_at = clean_text(data.get("starts_at"), "", 40)
    ends_at = clean_text(data.get("ends_at"), "", 40)
    status = clean_text(data.get("status"), "DRAFT", 16).upper()
    notes = clean_text(data.get("notes"), "", 500)
    event_id = clean_text(data.get("id"), existing_id or ("EVT-" + uuid.uuid4().hex[:8].upper()), 64)
    if not name:
        return None, "Event name is required"
    if status not in EVENT_STATUSES:
        return None, "Unsupported event status"
    return {
        "id": existing_id or event_id,
        "name": name,
        "venue_name": venue_name,
        "starts_at": starts_at,
        "ends_at": ends_at,
        "status": status,
        "notes": notes,
    }, None


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
        "event_id": row["event_id"] or "",
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
    }


def get_catalog(include_inactive: bool = False, event_id: str = "") -> list[dict[str, Any]]:
    clauses: list[str] = []
    params: list[Any] = []
    if not include_inactive:
        clauses.append("active = 1")
    if event_id:
        clauses.append("(event_id IS NULL OR event_id = '' OR event_id = ?)")
        params.append(event_id)
    sql = "SELECT * FROM catalog_items"
    if clauses:
        sql += " WHERE " + " AND ".join(clauses)
    sql += " ORDER BY category COLLATE NOCASE, name COLLATE NOCASE"
    with get_db() as conn:
        return [catalog_to_dict(row) for row in conn.execute(sql, params).fetchall()]


def find_catalog_item(*, item_id: str = "", name: str = "", barcode: str = "", active_only: bool = True, conn: sqlite3.Connection | None = None) -> dict[str, Any] | None:
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
    if conn is not None:
        row = conn.execute(sql, params).fetchone()
    else:
        with get_db() as database:
            row = database.execute(sql, params).fetchone()
    return catalog_to_dict(row) if row else None


def validate_catalog_payload(data: dict[str, Any], existing_id: str | None = None) -> tuple[dict[str, Any] | None, str | None]:
    name = clean_text(data.get("name"), max_length=120)
    category = clean_text(data.get("category"), "Event Services", 80)
    description = clean_text(data.get("description", data.get("desc")), "", 400)
    barcode = clean_text(data.get("barcode"), "", 128)
    item_type = clean_text(data.get("item_type"), "SERVICE", 24).upper()
    event_id = clean_text(data.get("event_id"), "", 64)
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
    if event_id and find_event(event_id) is None:
        return None, "Selected event does not exist"
    return {
        "id": existing_id or item_id,
        "name": name,
        "price": price,
        "category": category,
        "description": description,
        "barcode": barcode or None,
        "item_type": item_type,
        "event_id": event_id or None,
        "active": 1 if bool(data.get("active", True)) else 0,
    }, None


def device_to_dict(row: sqlite3.Row, current_ms: int | None = None) -> dict[str, Any]:
    now = current_ms if current_ms is not None else now_ms()
    last_seen = int(row["last_seen"])
    return {
        "id": row["id"],
        "device_name": row["device_name"],
        "manufacturer": row["manufacturer"],
        "model": row["model"],
        "android_version": row["android_version"],
        "sdk": row["sdk"],
        "app_version": row["app_version"],
        "operator_name": row["operator_name"],
        "shift_id": row["shift_id"],
        "printer_connected": bool(row["printer_connected"]),
        "unsynced_receipts": row["unsynced_receipts"],
        "remote_addr": row["remote_addr"],
        "first_seen": row["first_seen"],
        "last_seen": last_seen,
        "online": now - last_seen <= DEVICE_ONLINE_MS,
        "last_seen_seconds": max(0, (now - last_seen) // 1000),
    }


def order_to_dict(row: sqlite3.Row) -> dict[str, Any]:
    try:
        items = json.loads(row["items_json"])
    except (TypeError, json.JSONDecodeError):
        items = []
    return {
        "id": row["id"],
        "event_id": row["event_id"] or "",
        "target_device_id": row["target_device_id"] or "",
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


init_db()


@app.after_request
def add_security_headers(response):
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["X-Frame-Options"] = "SAMEORIGIN"
    if request.endpoint == "static":
        response.headers["Cache-Control"] = "public, max-age=3600"
    else:
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


@app.route("/events")
def events_page():
    return render_template("events.html")


@app.route("/devices")
def devices_page():
    return render_template("devices.html")


@app.route("/api/status", methods=["GET"])
def api_status():
    cutoff = now_ms() - DEVICE_ONLINE_MS
    with get_db() as conn:
        pending_count = conn.execute("SELECT COUNT(*) FROM orders WHERE status='PENDING'").fetchone()[0]
        total_count = conn.execute("SELECT COUNT(*) FROM orders").fetchone()[0]
        catalog_count = conn.execute("SELECT COUNT(*) FROM catalog_items WHERE active=1").fetchone()[0]
        barcode_count = conn.execute("SELECT COUNT(*) FROM catalog_items WHERE active=1 AND barcode IS NOT NULL AND barcode<>''").fetchone()[0]
        live_events = conn.execute("SELECT COUNT(*) FROM events WHERE status='LIVE'").fetchone()[0]
        connected_devices = conn.execute("SELECT COUNT(*) FROM devices WHERE last_seen>=?", (cutoff,)).fetchone()[0]
    return jsonify({
        "status": "ONLINE",
        "service": "Naomi-Chan™ DJ Company Event Operations",
        "currency": "GBP (£)",
        "default_vat": "20% UK VAT",
        "pending_orders": pending_count,
        "total_orders": total_count,
        "catalog_items": catalog_count,
        "barcode_items": barcode_count,
        "live_events": live_events,
        "connected_devices": connected_devices,
        "server_time": int(time.time()),
    })


@app.route("/api/devices", methods=["GET"])
def api_devices():
    with get_db() as conn:
        rows = conn.execute("SELECT * FROM devices ORDER BY last_seen DESC").fetchall()
    current = now_ms()
    return jsonify([device_to_dict(row, current) for row in rows])


@app.route("/api/devices/heartbeat", methods=["POST"])
def api_device_heartbeat():
    data = json_object()
    if data is None:
        return jsonify({"error": "Expected a JSON object"}), 400

    device_id = clean_text(data.get("device_id"), "", 64)
    if not device_id:
        return jsonify({"error": "device_id is required"}), 400

    stamp = now_ms()
    device_name = clean_text(data.get("device_name"), "Naomi-Chan PDA", 120)
    manufacturer = clean_text(data.get("manufacturer"), "", 80)
    model = clean_text(data.get("model"), "", 100)
    android_version = clean_text(data.get("android_version"), "", 32)
    app_version = clean_text(data.get("app_version"), "", 32)
    operator_name = clean_text(data.get("operator_name"), "", 120)
    shift_id = clean_text(data.get("shift_id"), "", 80)
    remote_addr = clean_text(request.remote_addr, "", 64)
    try:
        sdk = max(0, min(int(data.get("sdk", 0)), 1000))
        unsynced = max(0, min(int(data.get("unsynced_receipts", 0)), 100000))
    except (TypeError, ValueError):
        return jsonify({"error": "Invalid numeric device metadata"}), 400
    printer_connected = 1 if bool(data.get("printer_connected", False)) else 0

    with get_db() as conn:
        conn.execute(
            """INSERT INTO devices
               (id, device_name, manufacturer, model, android_version, sdk, app_version,
                operator_name, shift_id, printer_connected, unsynced_receipts,
                remote_addr, first_seen, last_seen)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT(id) DO UPDATE SET
                 device_name=excluded.device_name,
                 manufacturer=excluded.manufacturer,
                 model=excluded.model,
                 android_version=excluded.android_version,
                 sdk=excluded.sdk,
                 app_version=excluded.app_version,
                 operator_name=CASE WHEN excluded.operator_name<>'' THEN excluded.operator_name ELSE devices.operator_name END,
                 shift_id=CASE WHEN excluded.shift_id<>'' THEN excluded.shift_id ELSE devices.shift_id END,
                 printer_connected=excluded.printer_connected,
                 unsynced_receipts=excluded.unsynced_receipts,
                 remote_addr=excluded.remote_addr,
                 last_seen=excluded.last_seen""",
            (device_id, device_name, manufacturer, model, android_version, sdk, app_version,
             operator_name, shift_id, printer_connected, unsynced, remote_addr, stamp, stamp),
        )
        row = conn.execute("SELECT * FROM devices WHERE id=?", (device_id,)).fetchone()
        conn.commit()
    return jsonify(device_to_dict(row))


@app.route("/api/bars", methods=["GET"])
def api_bars():
    return jsonify(BLACKPOOL_BARS)


@app.route("/api/events", methods=["GET"])
def api_events():
    include_closed = request.args.get("include_closed", "0").lower() in {"1", "true", "yes"}
    return jsonify(get_events(include_closed=include_closed))


@app.route("/api/events", methods=["POST"])
def api_create_event():
    data = json_object()
    if data is None:
        return jsonify({"error": "Expected a JSON object"}), 400
    event, error = validate_event_payload(data)
    if error:
        return jsonify({"error": error}), 400
    assert event is not None
    stamp = now_ms()
    try:
        with get_db() as conn:
            conn.execute(
                """INSERT INTO events (id, name, venue_name, starts_at, ends_at, status, notes, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (event["id"], event["name"], event["venue_name"], event["starts_at"], event["ends_at"], event["status"], event["notes"], stamp, stamp),
            )
            conn.commit()
    except sqlite3.IntegrityError:
        return jsonify({"error": "Event ID already exists"}), 409
    return jsonify(find_event(event["id"])), 201


@app.route("/api/events/<event_id>", methods=["PUT"])
def api_update_event(event_id: str):
    safe_id = clean_text(event_id, max_length=64)
    data = json_object()
    if data is None:
        return jsonify({"error": "Expected a JSON object"}), 400
    event, error = validate_event_payload(data, existing_id=safe_id)
    if error:
        return jsonify({"error": error}), 400
    assert event is not None
    with get_db() as conn:
        cursor = conn.execute(
            """UPDATE events SET name=?, venue_name=?, starts_at=?, ends_at=?, status=?, notes=?, updated_at=? WHERE id=?""",
            (event["name"], event["venue_name"], event["starts_at"], event["ends_at"], event["status"], event["notes"], now_ms(), safe_id),
        )
        conn.commit()
    if cursor.rowcount == 0:
        return jsonify({"error": "Event not found"}), 404
    return jsonify(find_event(safe_id))


@app.route("/api/events/<event_id>", methods=["DELETE"])
def api_close_event(event_id: str):
    safe_id = clean_text(event_id, max_length=64)
    with get_db() as conn:
        cursor = conn.execute("UPDATE events SET status='CLOSED', updated_at=? WHERE id=?", (now_ms(), safe_id))
        conn.commit()
    if cursor.rowcount == 0:
        return jsonify({"error": "Event not found"}), 404
    return jsonify({"success": True, "id": safe_id, "status": "CLOSED"})


@app.route("/api/items", methods=["GET"])
def api_items():
    return jsonify(get_catalog(event_id=clean_text(request.args.get("event_id"), "", 64)))


@app.route("/api/catalog", methods=["GET"])
def api_catalog():
    include_inactive = request.args.get("include_inactive", "0").lower() in {"1", "true", "yes"}
    event_id = clean_text(request.args.get("event_id"), "", 64)
    return jsonify(get_catalog(include_inactive=include_inactive, event_id=event_id))


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
                   (id, name, price, category, description, barcode, item_type, active, created_at, updated_at, event_id)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (item["id"], item["name"], item["price"], item["category"], item["description"], item["barcode"], item["item_type"], item["active"], stamp, stamp, item["event_id"]),
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
                   SET name=?, price=?, category=?, description=?, barcode=?, item_type=?, active=?, updated_at=?, event_id=?
                   WHERE id=?""",
                (item["name"], item["price"], item["category"], item["description"], item["barcode"], item["item_type"], item["active"], now_ms(), item["event_id"], safe_id),
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
    device_id = clean_text(request.args.get("device_id"), "", 64)
    with get_db() as conn:
        if device_id:
            rows = conn.execute(
                """SELECT * FROM orders
                   WHERE status='PENDING'
                     AND (target_device_id IS NULL OR target_device_id='' OR target_device_id=?)
                   ORDER BY created_at ASC LIMIT 200""",
                (device_id,),
            ).fetchall()
        else:
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

    event_id = clean_text(data.get("event_id"), "", 64)
    target_device_id = clean_text(data.get("target_device_id"), "", 64)
    sanitized_items: list[dict[str, Any]] = []
    subtotal = 0.0

    with get_db() as conn:
        event = find_event(event_id, conn=conn) if event_id else None
        if event_id and event is None:
            return jsonify({"error": "Selected event was not found"}), 400
        if event and event["status"] == "CLOSED":
            return jsonify({"error": "Selected event is closed"}), 400
        if target_device_id:
            target = conn.execute("SELECT id FROM devices WHERE id=?", (target_device_id,)).fetchone()
            if target is None:
                return jsonify({"error": "Selected PDA is not registered with the gateway"}), 400

        for raw_item in raw_items:
            if not isinstance(raw_item, dict):
                return jsonify({"error": "Each line item must be an object"}), 400
            item_id = clean_text(raw_item.get("id"), max_length=64)
            name = clean_text(raw_item.get("name"), max_length=120)
            catalog_item = find_catalog_item(item_id=item_id, name=name, conn=conn)
            if catalog_item is None:
                return jsonify({"error": f"Unknown or inactive catalog item: {name or item_id or 'unnamed item'}"}), 400
            if catalog_item["event_id"] and catalog_item["event_id"] != event_id:
                return jsonify({"error": f"{catalog_item['name']} belongs to a different event"}), 400
            try:
                quantity = min(max(int(raw_item.get("quantity", 1)), 1), 99)
            except (TypeError, ValueError):
                return jsonify({"error": f"Invalid quantity for {catalog_item['name']}"}), 400
            unit_price = float(catalog_item["price"])
            subtotal += quantity * unit_price
            sanitized_items.append({
                "id": catalog_item["id"],
                "name": catalog_item["name"],
                "quantity": quantity,
                "unitPrice": unit_price,
                "itemType": catalog_item["item_type"],
                "barcode": catalog_item["barcode"],
            })

        subtotal = round(subtotal, 2)
        tax_percent = 20.0
        tax_amount = round(subtotal * tax_percent / 100.0, 2)
        grand_total = round(subtotal + tax_amount, 2)
        payment_method = clean_text(data.get("payment_method"), "CARD", 16).upper()
        if payment_method not in PAYMENT_METHODS:
            return jsonify({"error": "Unsupported payment method"}), 400

        order_id = "WPOS-" + uuid.uuid4().hex[:8].upper()
        client_name = clean_text(data.get("client_name"), "Event Guest", 120)
        client_contact = clean_text(data.get("client_contact"), "", 120)
        venue_name = clean_text(data.get("venue_name"), event["venue_name"] if event else "Naomi-Chan Event", 200)
        notes = clean_text(data.get("notes"), "Wireless event order", 500)
        stamp = now_ms()
        conn.execute(
            """INSERT INTO orders
               (id, event_id, target_device_id, client_name, client_contact, venue_name,
                items_json, subtotal, tax_percent, tax_amount, grand_total,
                payment_method, notes, status, created_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)""",
            (order_id, event_id or None, target_device_id or None, client_name, client_contact,
             venue_name, json.dumps(sanitized_items, separators=(",", ":")), subtotal,
             tax_percent, tax_amount, grand_total, payment_method, notes, stamp),
        )
        conn.commit()

    return jsonify({
        "success": True,
        "message": f"Event order {order_id} created",
        "order_id": order_id,
        "event_id": event_id,
        "target_device_id": target_device_id,
        "grand_total": grand_total,
        "status": "PENDING",
    }), 201


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
