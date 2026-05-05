# ...existing code...
from flask import Flask, request, jsonify
import os
import sys
import re
import logging
import MySQLdb
from email.utils import parseaddr

app = Flask(__name__)

# ...existing code...

def _get_db_conn():
    """Create a DB connection using environment variables. Fail early if not configured."""
    db_host = os.environ.get("DB_HOST")
    db_user = os.environ.get("DB_USER")
    db_pass = os.environ.get("DB_PASS")
    db_name = os.environ.get("DB_NAME")
    if not (db_host and db_user and db_pass and db_name):
        logging.error("Database credentials are not fully configured in environment variables.")
        raise RuntimeError("Database not configured")
    # Use utf8mb4 to support full unicode; use parameterized queries only.
    return MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset="utf8mb4", use_unicode=True)

def _is_valid_email(addr: str) -> bool:
    """Simple, safe email validation: non-empty, reasonable length, and basic structure.
    Do not rely on this for absolute RFC conformance; it prevents obviously invalid inputs."""
    if not isinstance(addr, str):
        return False
    addr = addr.strip()
    if not addr or len(addr) > 254:
        return False
    # parseaddr returns ('Display Name', 'local@domain')
    realname, email = parseaddr(addr)
    if not email:
        return False
    # basic regex: something@something.tld (prevents injection & control chars)
    pattern = re.compile(r'^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$')
    return bool(pattern.match(email))

@app.route('/unsubscribe', methods=['POST'])
def unsubscribe():
    """Unsubscribe endpoint. Accepts JSON or form POST with an 'email' field.
    Returns generic responses to avoid leaking whether an address exists in the DB."""
    # Accept only POST for state changes
    data = {}
    if request.is_json:
        data = request.get_json(silent=True) or {}
    else:
        # support form submissions as a fallback
        data = request.form or {}

    email = data.get("email", "")
    if not _is_valid_email(email):
        # Do not echo user input; return a generic message.
        return jsonify({"status": "error", "message": "Invalid request"}), 400

    # Enforce a reasonable maximum length on the input to avoid resource abuse
    email = email.strip()
    if len(email) > 254:
        return jsonify({"status": "error", "message": "Invalid request"}), 400

    try:
        conn = _get_db_conn()
    except Exception:
        # Do not leak internal details
        logging.exception("Failed to obtain DB connection")
        return jsonify({"status": "error", "message": "Service temporarily unavailable"}), 503

    try:
        cursor = conn.cursor()
        try:
            # Use parameterized queries to prevent SQL injection
            cursor.execute("SELECT id, unsubscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
            row = cursor.fetchone()
            if not row:
                # Generic success message to avoid confirming existence
                return jsonify({"status": "ok", "message": "If that address is in our system, it has been unsubscribed."}), 200

            subscriber_id, unsubscribed_flag = row[0], row[1]
            if unsubscribed_flag:
                return jsonify({"status": "ok", "message": "If that address is in our system, it has been unsubscribed."}), 200

            cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE id = %s", (subscriber_id,))
            conn.commit()
            return jsonify({"status": "ok", "message": "If that address is in our system, it has been unsubscribed."}), 200
        finally:
            cursor.close()
    except Exception:
        logging.exception("Database operation failed")
        # Generic error, do not expose internals
        return jsonify({"status": "error", "message": "Service temporarily unavailable"}), 500
    finally:
        try:
            conn.close()
        except Exception:
            pass

# ...existing code...
