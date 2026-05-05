from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # read and validate input
    email = (request.args.get('email') or '').strip()
    if not email:
        return jsonify({'error': 'missing email parameter'}), 400

    EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$")
    if not EMAIL_RE.match(email):
        return jsonify({'error': 'invalid email format'}), 400

    # DB config from environment (do not hardcode credentials)
    db_cfg = {
        'host': os.environ.get('DB_HOST', '127.0.0.1'),
        'user': os.environ.get('DB_USER', 'root'),
        'passwd': os.environ.get('DB_PASS', ''),
        'db': os.environ.get('DB_NAME', 'test'),
        'charset': 'utf8mb4',
        'use_unicode': True,
    }

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(**db_cfg)
        cur = conn.cursor()

        # Parameterized queries to prevent SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return jsonify({'status': 'not_found', 'message': 'Email not found'}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({'status': 'already_unsubscribed', 'message': 'Email already unsubscribed'}), 200

        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        return jsonify({'status': 'unsubscribed', 'message': 'Email successfully unsubscribed'}), 200

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