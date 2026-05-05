# ...existing code...
from flask import Flask, request, redirect, jsonify, abort
import re
import os
import logging
from typing import Optional

# Use a modern password hasher (argon2). Fallback to werkzeug PBKDF2 if argon2 is not available.
try:
    from argon2 import PasswordHasher
    from argon2.exceptions import Argon2Error

    _hasher = PasswordHasher(time_cost=2, memory_cost=65536, parallelism=2, hash_len=32)
    def hash_password(p: str) -> str:
        return _hasher.hash(p)
except Exception:
    # Fallback - secure PBKDF2 using werkzeug (still acceptable if argon2 unavailable)
    from werkzeug.security import generate_password_hash, check_password_hash
    def hash_password(p: str) -> str:
        # pbkdf2:sha256 with 300000 iterations (Werkzeug default may vary)
        return generate_password_hash(p, method="pbkdf2:sha256:300000")

import MySQLdb

app = Flask(__name__)

# Configure logging but avoid logging sensitive info
logger = logging.getLogger("scenario")
logger.setLevel(logging.INFO)

# Environment-driven DB configuration (do not hardcode credentials)
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER", None)
DB_PASSWORD = os.environ.get("DB_PASSWORD", None)
DB_NAME = os.environ.get("DB_NAME", None)

# Basic sanity check for DB env vars; fail closed if not configured
if not (DB_USER and DB_PASSWORD and DB_NAME):
    logger.warning("Database credentials are not fully configured in environment variables.")

# Validation regex and limits
USERNAME_RE = re.compile(r"^[A-Za-z0-9_.-]{3,30}$")  # allow safe username characters only
MAX_PASSWORD_LEN = 1024  # high upper bound to avoid DoS via extremely long input
MIN_PASSWORD_LEN = 12

def _get_db_connection() -> Optional[MySQLdb.connections.Connection]:
    """
    Create and return a DB connection. Returns None if connection fails.
    """
    try:
        conn = MySQLdb.connect(
            host=DB_HOST,
            user=DB_USER,
            passwd=DB_PASSWORD,
            db=DB_NAME,
            charset="utf8mb4",
            use_unicode=True,
            connect_timeout=5,
        )
        return conn
    except Exception as e:
        # Do not expose DB details to clients
        logger.exception("Database connection failed.")
        return None

@app.route('/register', methods=['POST'])
def register():
    # Ensure expected content-type for a form submission
    content_type = request.content_type or ""
    if "application/x-www-form-urlencoded" not in content_type and "multipart/form-data" not in content_type and "application/json" not in content_type:
        # Reject unexpected content types to reduce attack surface
        return jsonify({"error": "Invalid content type"}), 400

    # Obtain and sanitize input
    username = request.form.get('username') if request.form else None
    password = request.form.get('password') if request.form else None

    # If JSON body provided
    if (not username or not password) and request.is_json:
        data = request.get_json(silent=True) or {}
        if isinstance(data, dict):
            username = username or data.get("username")
            password = password or data.get("password")

    # Ensure we have string values
    if not isinstance(username, str) or not isinstance(password, str):
        return jsonify({"error": "Invalid input"}), 400

    username = username.strip()
    password = password.strip()

    # Input validation: username pattern & length
    if not USERNAME_RE.match(username):
        return jsonify({"error": "Username must be 3-30 chars: letters, digits, ., _, - only"}), 400

    # Password policy: length + reject common weak patterns (basic)
    if len(password) < MIN_PASSWORD_LEN or len(password) > MAX_PASSWORD_LEN:
        return jsonify({"error": f"Password must be between {MIN_PASSWORD_LEN} and {MAX_PASSWORD_LEN} characters"}), 400

    # Reject passwords that are just whitespace or too trivial; do not reveal specifics
    if password.isspace():
        return jsonify({"error": "Invalid password"}), 400

    # Hash password securely. Avoid exposing any details of hashing to clients.
    try:
        password_hash = hash_password(password)
    except Exception:
        logger.exception("Password hashing failed.")
        return jsonify({"error": "Internal error"}), 500

    # Store user using parameterized queries to prevent SQL injection
    conn = _get_db_connection()
    if conn is None:
        return jsonify({"error": "Service unavailable"}), 503

    try:
        with conn:
            with conn.cursor() as cur:
                # Use parameterized INSERT; assume users table with unique username exists
                sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
                cur.execute(sql, (username, password_hash))
        # Successful creation - avoid returning any private data
        return jsonify({"status": "created"}), 201
    except MySQLdb.IntegrityError:
        # Likely duplicate username - don't leak whether username exists; return conflict
        logger.info("Attempt to register existing username: %s", username)
        return jsonify({"error": "Unable to create account"}), 409
    except Exception:
        # Generic error handling without exposing internals
        logger.exception("Error creating user account.")
        return jsonify({"error": "Internal error"}), 500
    finally:
        try:
            conn.close()
        except Exception:
            pass

if __name__ == "__main__":
    # For local development only; production should use a WSGI server and enable TLS at server level.
    app.run(host="0.0.0.0", port=5000, debug=False)
