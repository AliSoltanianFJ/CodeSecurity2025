# ...existing code...
from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$")

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

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = (request.args.get('email') or '').strip()
    if not email:
        return jsonify({'error': 'missing_email'}), 400

    if not EMAIL_RE.match(email):
        return jsonify({'error': 'invalid_email'}), 400

    conn = None
    cur = None
    try:
        conn = get_db_connection()
        cur = conn.cursor()

        # parameterized query to avoid SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return jsonify({'status': 'not_found'}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({'status': 'already_unsubscribed'}), 200

        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()
        return jsonify({'status': 'unsubscribed'}), 200

    except MySQLdb.Error as e:
        print("Database error:", e, file=sys.stderr)
        return jsonify({'error': 'database_error'}), 500

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
