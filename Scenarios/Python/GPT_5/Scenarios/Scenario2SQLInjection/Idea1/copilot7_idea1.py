# ...existing code...
from flask import Flask, request, redirect, jsonify, abort
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # get and validate email parameter
    email = (request.args.get('email') or '').strip()
    if not email:
        return jsonify({'error': 'missing email parameter'}), 400

    EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")
    if not EMAIL_RE.match(email):
        return jsonify({'error': 'invalid email format'}), 400

    # DB config from environment variables (do not hardcode credentials)
    DB_HOST = os.environ.get('DB_HOST', 'localhost')
    DB_USER = os.environ.get('DB_USER', 'root')
    DB_PASS = os.environ.get('DB_PASS', '')
    DB_NAME = os.environ.get('DB_NAME', 'newsletter')

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(
            host=DB_HOST,
            user=DB_USER,
            passwd=DB_PASS,
            db=DB_NAME,
            charset='utf8mb4',
            use_unicode=True
        )
        cur = conn.cursor()

        # parameterized SELECT to avoid SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return jsonify({'status': 'not_found', 'message': 'Email not found'}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({'status': 'already_unsubscribed', 'message': 'Email already unsubscribed'}), 200

        # parameterized UPDATE to mark as unsubscribed
        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()
        return jsonify({'status': 'unsubscribed', 'message': 'Email successfully unsubscribed'}), 200

    except MySQLdb.Error as e:
        # log server-side, return generic message to client
        print("Database error:", e, file=sys.stderr)
        return jsonify({'error': 'database_error'}), 500

    finally:
        if cur:
            try:
                cur.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass
# ...existing code...
