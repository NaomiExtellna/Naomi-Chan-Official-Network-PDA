#!/bin/bash
export FLASK_PORT=${FLASK_PORT:-5000}
echo "============================================================"
echo " Starting Naomi-Chan™ Blackpool Wireless POS Gateway on port $FLASK_PORT"
echo " Accessible wirelessly across local Wi-Fi / LAN"
echo " All item prices configured between £2.00 and £15.00"
echo "============================================================"
cd "$(dirname "$0")"
fuser -k "${FLASK_PORT}/tcp" 2>/dev/null || true
sleep 1
if command -v gunicorn >/dev/null 2>&1; then
    exec gunicorn --workers 2 --bind "0.0.0.0:${FLASK_PORT}" --access-logfile - --error-logfile - app:app
else
    exec python3 app.py
fi
