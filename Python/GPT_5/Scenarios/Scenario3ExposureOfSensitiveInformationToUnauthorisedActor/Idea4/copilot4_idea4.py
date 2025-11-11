# ...existing code...
from flask import Flask, request, redirect, session, abort
import os
import hashlib
import re
import binascii
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET')
# ...existing code...
"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    if not isinstance(username, str) or not isinstance(password, str):
        abort(400)

    if not (3 <= len(username) <= 150 and 8 <= len(password) <= 128):
        abort(400)

    if not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        abort(400)

    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    if not all([db_host, db_user, db_pass, db_name]):
        abort(503)

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cur = conn.cursor()
        cur.execute(
            "SELECT id, password_hash, salt, iterations FROM users WHERE username = %s LIMIT 1",
            (username,)
        )
        row = cur.fetchone()
    except Exception:
        abort(503)
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

    if not row:
        abort(401)

    user_id, stored_hash_hex, salt_hex, iterations = row

    if not stored_hash_hex or not salt_hex:
        abort(401)

    try:
        salt = bytes.fromhex(salt_hex)
        stored_hash = bytes.fromhex(stored_hash_hex)
        iterations = int(iterations) if iterations else 100000
        derived = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
    except Exception:
        abort(401)

    if not hashlib.compare_digest(derived, stored_hash):
        abort(401)

    session.clear()
    session['user_id'] = user_id
    return redirect('/')
# ...existing code...
