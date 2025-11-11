# ...existing code...
from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb
import re
from werkzeug.security import check_password_hash, generate_password_hash

app = Flask(__name__)

# Ensure a secret key is set (prefer from environment in production)
app.secret_key = os.environ.get("FLASK_SECRET_KEY") or os.urandom(32)
app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_SAMESITE="Lax",
)

# Pre-compute a fake password hash to mitigate timing/user-enumeration attacks
_FAKE_PASSWORD_HASH = generate_password_hash("fake-password-for-timing")

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Validate presence and types
    username = request.form.get('username', '')
    password = request.form.get('password', '')
    if not isinstance(username, str) or not isinstance(password, str):
        return "Invalid input", 400

    # Normalize and basic size limits to prevent large payloads/buffer issues
    username = username.strip()
    if not username or not password:
        return "Invalid username or password", 400
    if len(username) > 150 or len(password) > 128:
        return "Invalid username or password", 400

    # Allow a conservative set of username characters to reduce injection/XSS risk
    if not re.fullmatch(r'[A-Za-z0-9._@-]{1,150}', username):
        return "Invalid username or password", 400

    # Use environment-configured DB credentials; do not hardcode in production
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', 'root')
    db_name = os.environ.get('DB_NAME', 'db')

    conn = None
    cursor = None
    try:
        # Use parameterized queries to prevent SQL injection
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cursor = conn.cursor()
        cursor.execute("SELECT password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cursor.fetchone()

        if row and row[0]:
            stored_hash = row[0]
            user_exists = True
        else:
            # Use fake hash to make timing similar whether or not user exists
            stored_hash = _FAKE_PASSWORD_HASH
            user_exists = False

        # Compare using secure hash verification (constant-time where possible)
        password_ok = check_password_hash(stored_hash, password)

        if password_ok and user_exists:
            # Successful login: set minimal session data
            session.clear()
            session['username'] = username
            # Redirect to a safe internal page; do not reflect user input
            return redirect('/')
        else:
            # Generic error message to avoid revealing which part failed
            return "Invalid username or password", 401

    except Exception:
        # Do not leak internal errors to the client; log server-side for diagnostics
        app.logger.exception("Error during login")
        return "An error occurred", 500

    finally:
        try:
            if cursor:
                cursor.close()
        except Exception:
            pass
        try:
            if conn:
                conn.close()
        except Exception:
            pass
# ...existing code...
