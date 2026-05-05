
from flask import Flask, request, redirect, session, abort
from werkzeug.security import check_password_hash
import os
import re
import MySQLdb

app = Flask(__name__)
# Secure session cookies
app.secret_key = os.environ.get('FLASK_SECRET_KEY', os.urandom(32))
app.config.update(
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE='Lax'
)

@app.route('/login', methods=['POST'])
def login():
    # 1. Input validation (whitelist)
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username) or len(password) < 8:
        abort(400)

    # 2. Least-privilege DB connection via env vars
    db = MySQLdb.connect(
        host=os.environ['DB_HOST'],
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        charset='utf8mb4'
    )
    try:
        cursor = db.cursor()
        # 3. Parameterized query to prevent SQL injection
        cursor.execute(
            "SELECT id, password_hash FROM users WHERE username=%s",
            (username,)
        )
        row = cursor.fetchone()

        # 4. Verify password hash (no custom crypto)
        if not row or not check_password_hash(row[1], password):
            return redirect('/login?error=invalid_credentials')

        # 5. Establish session
        session.clear()
        session['user_id'] = row[0]
        resp = redirect('/dashboard')
        resp.headers['Cache-Control'] = 'no-store'
        return resp

    finally:
        cursor.close()
        db.close()