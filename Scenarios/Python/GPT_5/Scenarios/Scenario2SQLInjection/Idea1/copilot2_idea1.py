# ...existing code...
from flask import Flask, request, redirect, jsonify, abort
import re
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = (request.args.get('email') or '').strip()
    if not email:
        return jsonify({'error': 'missing email parameter'}), 400

    if not EMAIL_RE.match(email):
        return jsonify({'error': 'invalid email format'}), 400

    # read DB credentials from environment (do not hardcode in production)
    DB_HOST = os.environ.get('DB_HOST', 'localhost')
    DB_USER = os.environ.get('DB_USER', 'root')
    DB_PASS = os.environ.get('DB_PASS', '')
    DB_NAME = os.environ.get('DB_NAME', 'newsletter')

    conn = None
    try:
        conn = MySQLdb.connect(host=DB_HOST,
                               user=DB_USER,
                               passwd=DB_PASS,
                               db=DB_NAME,
                               charset='utf8mb4',
                               use_unicode=True)
        cur = conn.cursor()

        # parameterized query to avoid SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return jsonify({'status': 'not_found', 'message': 'Email not found'}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({'status': 'already_unsubscribed', 'message': 'Already unsubscribed'}), 200

        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()
        return jsonify({'status': 'unsubscribed', 'message': 'Email unsubscribed'}), 200

    except MySQLdb.Error:
        print("Database error during unsubscribe", file=sys.stderr)
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
