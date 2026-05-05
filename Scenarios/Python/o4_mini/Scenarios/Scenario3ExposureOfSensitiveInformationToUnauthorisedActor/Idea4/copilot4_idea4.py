
import re
import os
import hmac
import hashlib
from datetime import timedelta
from flask import Flask, request, redirect, session, abort
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY') or os.urandom(32)
app.config.update(
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE='Lax',
    PERMANENT_SESSION_LIFETIME=timedelta(minutes=30),
    DEBUG=False
)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 1. Input validation (whitelist)
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)

    # 2. Least‐privilege DB connection, creds from env
    conn = MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER', 'app_user'),
        passwd=os.environ.get('DB_PASSWORD'),
        db=os.environ.get('DB_NAME', 'appdb'),
        charset='utf8mb4',
        sql_mode='STRICT_ALL_TABLES'
    )
    try:
        cursor = conn.cursor()
        # 3. Parameterized query
        cursor.execute(
            'SELECT password_hash, salt FROM users WHERE username=%s',
            (username,)
        )
        row = cursor.fetchone()
        if not row:
            abort(401)  # generic error

        stored_hash, salt = row
        # 4. PBKDF2‐HMAC password check
        computed = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt.encode('utf-8'),
            100_000
        )
        if not hmac.compare_digest(stored_hash.encode('utf-8'), computed):
            abort(401)

        # 5. Establish session
        session.clear()
        session['user'] = username
        session.permanent = True
        return redirect('/dashboard')

    except MySQLdb.Error:
        abort(500)
    finally:
        cursor.close()
        conn.close()