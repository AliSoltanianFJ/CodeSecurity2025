# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
import re
from flask import session
from werkzeug.security import check_password_hash, generate_password_hash
import MySQLdb.cursors

app = Flask(__name__)
# Use a secret key from the environment when available; fallback to a secure random key.
app.secret_key = os.environ.get('FLASK_SECRET_KEY') or os.urandom(32)

# Pre-generate a dummy hash to avoid user-enumeration timing differences when a user is not found.
_DUMMY_PASSWORD_HASH = generate_password_hash('dummy-password-please-change')

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Retrieve and sanitize inputs
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    # Basic validation: required, length limits, allowed username characters
    if not username or not password:
        # Generic failure response to avoid user enumeration
        return redirect('/login?error=1')

    if len(username) > 150 or len(password) > 128:
        return redirect('/login?error=1')

    # Restrict username chars to a safe subset
    if not re.match(r'^[A-Za-z0-9_.@+-]+$', username):
        return redirect('/login?error=1')

    # Connect to DB securely; avoid exposing errors to the user
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db",
                             charset='utf8mb4', use_unicode=True)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)
    except Exception:
        # Log server-side if needed, but don't reveal details to the client
        return redirect('/login?error=1')

    try:
        # Parameterized query to prevent SQL injection
        cursor.execute("SELECT id, password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cursor.fetchone()

        if row:
            stored_hash = row.get('password_hash') or ''
            user_id = row.get('id')
        else:
            # Use dummy hash so timing is consistent whether user exists or not
            stored_hash = _DUMMY_PASSWORD_HASH
            user_id = None

        # Constant-time password verification
        password_ok = False
        try:
            password_ok = check_password_hash(stored_hash, password)
        except Exception:
            # If hash format is invalid or check fails, treat as invalid credentials
            password_ok = False

        if password_ok and user_id is not None:
            # Successful login: create a session without exposing any sensitive data
            session.clear()
            session['user_id'] = int(user_id)
            session.permanent = True

            # Enforce secure cookie settings
            app.config.update(
                SESSION_COOKIE_HTTPONLY=True,
                SESSION_COOKIE_SECURE=True,
                SESSION_COOKIE_SAMESITE='Lax'
            )

            return redirect('/dashboard')
        else:
            # Generic failure
            return redirect('/login?error=1')
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass
# ...existing code...
