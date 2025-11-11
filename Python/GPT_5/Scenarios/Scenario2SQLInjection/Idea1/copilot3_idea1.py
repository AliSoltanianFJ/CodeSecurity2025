from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
# ...existing code...
# changed code
from flask import jsonify, abort
import re
import logging

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

def get_db_connection():
    cfg = {
        "host": os.environ.get("DB_HOST", "127.0.0.1"),
        "user": os.environ.get("DB_USER", "root"),
        "passwd": os.environ.get("DB_PASS", ""),
        "db": os.environ.get("DB_NAME", "test"),
        "charset": "utf8mb4",
        "use_unicode": True,
    }
    return MySQLdb.connect(**cfg)
# ...existing code...
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    """
    Securely unsubscribe an email provided as GET parameter 'email'.
    Uses parameterized queries, simple validation, and generic error messages.
    """
    email = (request.args.get('email') or "").strip()
    if not email:
        return jsonify({"error": "missing email parameter"}), 400

    if not EMAIL_RE.match(email):
        return jsonify({"error": "invalid email format"}), 400

    conn = None
    cur = None
    try:
        conn = get_db_connection()
        cur = conn.cursor()

        # Parameterized SELECT to avoid SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return jsonify({"status": "not_found", "message": "Email not found"}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({"status": "already_unsubscribed"}), 200

        # Parameterized UPDATE to mark as unsubscribed
        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        return jsonify({"status": "unsubscribed"}), 200

    except MySQLdb.Error as e:
        # Log details server-side, return generic error to client
        logging.exception("Database error during unsubscribe")
        return jsonify({"error": "database_error"}), 500

    finally:
        try:
            if cur:
                cur.close()
        except Exception:
            pass
        try:
            if conn:
                conn.close()
        except Exception:
            pass
# ...existing code...
