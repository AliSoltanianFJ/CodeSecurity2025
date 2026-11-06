import os
import re
from flask import Flask, request, redirect, url_for, abort
from werkzeug.security import generate_password_hash
import mysql.connector
from mysql.connector import errors as db_errors

app = Flask(__name__)

# Ensure DEBUG is disabled in production
app.config['DEBUG'] = False

# Database configuration is read-only at runtime and supplied via environment variables.
DB_CONFIG = {
    'host':     os.environ.get('DB_HOST', ''),
    'user':     os.environ.get('DB_USER', ''),
    'password': os.environ.get('DB_PASSWORD', ''),
    'database': os.environ.get('DB_NAME', ''),
    # If you use TLS/SSL for MySQL, point to CA bundle:
    # 'ssl_ca': os.environ.get('DB_SSL_CA_FILE', None),
    # 'ssl_verify_cert': True,
}

USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,30}$')  # allow only letters, digits, underscore, length 3–30
PASSWORD_MIN_LEN = 8
PASSWORD_MAX_LEN = 128

@app.route('/register', methods=['POST'])
def register():
    # 1) Enforce HTTPS for sensitive operations
    if not request.is_secure:
        # Reject clear-text requests
        abort(400)

    # 2) Retrieve and normalize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 3) Basic presence check
    if not username or not password:
        # Generic error, no details leaked
        abort(400)

    # 4) Validate username against whitelist
    if not USERNAME_REGEX.fullmatch(username):
        abort(400)

    # 5) Enforce password policy
    if not (PASSWORD_MIN_LEN <= len(password) <= PASSWORD_MAX_LEN):
        abort(400)

    # 6) Hash the password with a strong adaptive algorithm and random salt
    try:
        # PBKDF2-HMAC-SHA256, 260,000 iterations (default as of Werkzeug 2.x), 16-byte random salt
        password_hash = generate_password_hash(
            password,
            method='pbkdf2:sha256',
            salt_length=16
        )
    except Exception:
        # If hashing fails for any reason, do not proceed
        app.logger.error("Password hashing failed", exc_info=True)
        abort(500)

    # 7) Store into database using a short‐lived connection and prepared statements
    conn = None
    cursor = None
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        # Use server‐side prepared statements
        cursor = conn.cursor(prepared=True)

        # 7a) Check for existing username
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s LIMIT 1",
            (username,)
        )
        if cursor.fetchone():
            # Conflict – do not reveal which field duplicated
            abort(409)

        # 7b) Insert new user
        cursor.execute(
            "INSERT INTO users (username, password_hash, created_at) VALUES (%s, %s, NOW())",
            (username, password_hash)
        )
        conn.commit()

    except db_errors.Error:
        # Log internal error without revealing details to user
        app.logger.error("Database error during registration", exc_info=True)
        abort(500)

    finally:
        # 8) Clean up resources
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    # 9) On success, redirect to login
    return redirect(url_for('login'))


# Optional: generic login page placeholder
@app.route('/login', methods=['GET'])
def login():
    # Render your login form template here; ensure any user-supplied values are escaped
    return "Please log in."