from flask import Flask, request, redirect, session, abort
import MySQLdb
import hashlib
import hmac
import os

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', os.urandom(24))

@app.route('/login', methods=['POST'])
def login():
    # 1. Grab and sanity-check inputs
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''
    if not username or not password:
        abort(400, 'Username and password required')

    try:
        # 2. Connect and safely query for stored hash (and optional salt)
        db = MySQLdb.connect(host="localhost", user="root",
                             passwd="root", db="db",
                             charset='utf8mb4', use_unicode=True)
        cur = db.cursor()
        # Assume table users(username, password_hash, salt)
        cur.execute(
            "SELECT password_hash, salt FROM users WHERE username = %s",
            (username,)
        )
        row = cur.fetchone()
        cur.close()
        db.close()
    except MySQLdb.Error:
        # never leak details
        abort(500, 'Internal server error')

    if not row:
        # generic message on missing user
        abort(401, 'Invalid credentials')

    stored_hash, salt = row
    # 3. Recompute hash: here using PBKDF2-HMAC-SHA256
    computed_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        100_000
    ).hex()

    # 4. Constant-time compare
    if not hmac.compare_digest(computed_hash, stored_hash):
        abort(401, 'Invalid credentials')

    # 5. Log in user
    session.clear()
    session['username'] = username
    return redirect('/dashboard')