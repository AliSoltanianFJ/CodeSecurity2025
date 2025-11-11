from flask import Flask, request, jsonify, abort
from werkzeug.security import generate_password_hash
import re
import MySQLdb
import os
import logging

app = Flask(__name__)

# Configuration / limits
MAX_USERNAME_LEN = 150
MIN_USERNAME_LEN = 3
MAX_PASSWORD_LEN = 128
MIN_PASSWORD_LEN = 12
USERNAME_RE = re.compile(r'^[A-Za-z0-9_.-]+$')  # restrict allowed characters

logging.basicConfig(level=logging.INFO)


def _get_db_connection():
    """Create a secure DB connection using environment variables. Do not log secrets."""
    db_host = os.environ.get('MYSQL_HOST', '127.0.0.1')
    db_user = os.environ.get('MYSQL_USER')
    db_pass = os.environ.get('MYSQL_PASSWORD')
    db_name = os.environ.get('MYSQL_DATABASE')

    if not (db_user and db_pass and db_name):
        logging.error("Database credentials are not fully set in environment variables.")
        abort(500, description="Server configuration error")

    # Use utf8mb4 for full Unicode support and avoid character-based issues
    connect_kwargs = {
        'host': db_host,
        'user': db_user,
        'passwd': db_pass,
        'db': db_name,
        'charset': 'utf8mb4',
        'use_unicode': True,
        'connect_timeout': 5
    }

    # Optional: if SSL CA path provided, use it
    ssl_ca = os.environ.get('MYSQL_SSL_CA')
    if ssl_ca:
        connect_kwargs['ssl'] = {'ca': ssl_ca}

    return MySQLdb.connect(**connect_kwargs)


def _validate_username(username: str) -> bool:
    if not username:
        return False
    username = username.strip()
    if not (MIN_USERNAME_LEN <= len(username) <= MAX_USERNAME_LEN):
        return False
    if not USERNAME_RE.fullmatch(username):
        return False
    return True


def _validate_password(password: str) -> bool:
    if not password:
        return False
    if not (MIN_PASSWORD_LEN <= len(password) <= MAX_PASSWORD_LEN):
        return False
    # Basic complexity checks: uppercase, lowercase, digit, special
    checks = [
        re.search(r'[A-Z]', password),
        re.search(r'[a-z]', password),
        re.search(r'\d', password),
        re.search(r'[^A-Za-z0-9]', password)
    ]
    return all(checks)


@app.route('/register', methods=['POST'])
def register():
    # Require HTTPS in production
    if not request.is_secure and os.environ.get('REQUIRE_SECURE', '1') == '1':
        # Do not expose server details. Encourage use of HTTPS.
        abort(403, description="Secure connection required")

    # Get and sanitize inputs
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Basic trimming to avoid hidden whitespace attacks
    username = username.strip()

    # Validate inputs
    if not _validate_username(username):
        return jsonify({"error": "Invalid username or username length not allowed"}), 400

    if not _validate_password(password):
        return jsonify({"error": "Password does not meet complexity or length requirements"}), 400

    # Hash password using a modern, salted PBKDF2 (werkzeug uses a secure default)
    try:
        password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    except Exception:
        logging.exception("Password hashing failed")
        abort(500, description="Internal server error")

    # Store user securely using parameterized queries to prevent SQL injection
    conn = None
    try:
        conn = _get_db_connection()
        cursor = conn.cursor()

        # Check whether user exists (do not reveal existence in response)
        cursor.execute("SELECT 1 FROM users WHERE username = %s LIMIT 1", (username,))
        exists = cursor.fetchone() is not None

        if exists:
            # To avoid username enumeration, respond with a generic message but return 200/409 as desired.
            # Here we return 409 conflict to indicate registration cannot proceed while keeping message generic.
            return jsonify({"error": "Registration could not be completed"}), 409

        # Insert the new user record safely
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
        cursor.close()
    except MySQLdb.DatabaseError:
        logging.exception("Database operation failed")
        # Generic error message; don't expose DB details
        if conn:
            try:
                conn.rollback()
            except Exception:
                pass
        abort(500, description="Internal server error")
    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    # Success response (do not echo sensitive inputs)
    return jsonify({"status": "registration submitted"}), 201


if __name__ == '__main__':
    # In development allow running without HTTPS, but REQUIRE_SECURE env var should be set in prod.
    debug_mode = os.environ.get('FLASK_DEBUG', '0') == '1'
    app.run(host='0.0.0.0', port=5000, debug=debug_mode)
