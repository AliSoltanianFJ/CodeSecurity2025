# ...existing code...
from flask import Flask, request, redirect, abort, jsonify
import re
import os
import logging
import MySQLdb
from werkzeug.security import generate_password_hash
import unicodedata

app = Flask(__name__)

# Basic app security hardening (example; adjust to your deployment)
app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,
)

# Configure logging (do not log secrets)
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# Input limits and validation patterns
USERNAME_RE = re.compile(r'^[A-Za-z0-9_.-]{3,30}$')
PASSWORD_MIN_LENGTH = 8
PASSWORD_MAX_LENGTH = 128
# Require at least one upper, one lower, one digit and one symbol
PASSWORD_COMPLEXITY_RE = re.compile(r'(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W)')

def get_db_connection():
    """
    Get a DB connection using environment variables. Do not hardcode credentials.
    Raises RuntimeError if required env vars are missing.
    """
    host = os.environ.get('DB_HOST')
    user = os.environ.get('DB_USER')
    password = os.environ.get('DB_PASS')
    db = os.environ.get('DB_NAME')
    if not all([host, user, password, db]):
        logger.error("Database credentials are not fully configured in environment")
        raise RuntimeError("Database not configured")
    # use unicode and utf8mb4 to avoid charset issues
    return MySQLdb.connect(host=host, user=user, passwd=password, db=db, charset='utf8mb4', use_unicode=True)

@app.route('/register', methods=['POST'])
def register():
    """
    Secure user registration endpoint:
    - Validates and sanitizes input
    - Enforces length and complexity limits
    - Hashes password with a modern KDF (werkzeug PBKDF2-SHA256)
    - Uses parameterized queries to avoid SQL injection
    - Does not leak internal errors or sensitive data
    """
    try:
        # Read and normalise inputs
        raw_username = request.form.get('username', '')
        raw_password = request.form.get('password', '')
        if raw_username is None or raw_password is None:
            # missing form fields
            return jsonify({"error": "Invalid request"}), 400

        username = unicodedata.normalize('NFKC', raw_username).strip()
        password = raw_password.strip()

        # Enforce size limits (prevents overly large payloads / memory issues)
        if not (1 <= len(username) <= 300) or not (1 <= len(password) <= 10000):
            return jsonify({"error": "Invalid request"}), 400

        # Validate username with a strict allowlist (prevents XSS, injections)
        if not USERNAME_RE.fullmatch(username):
            return jsonify({"error": "Username must be 3-30 characters and contain only letters, numbers, dot, underscore or hyphen"}), 400

        # Validate password length and complexity
        if not (PASSWORD_MIN_LENGTH <= len(password) <= PASSWORD_MAX_LENGTH):
            return jsonify({"error": f"Password must be {PASSWORD_MIN_LENGTH}-{PASSWORD_MAX_LENGTH} characters long"}), 400
        if not PASSWORD_COMPLEXITY_RE.search(password):
            return jsonify({"error": "Password must include upper and lower case letters, digits and symbols"}), 400

        # Hash password with a strong modern algorithm (PBKDF2-SHA256 via werkzeug)
        # salt_length default is secure; method includes iterations
        password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

        # Store user using parameterized queries to avoid SQL injection
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                # Check if username already exists
                cur.execute("SELECT 1 FROM users WHERE username = %s LIMIT 1", (username,))
                if cur.fetchone():
                    # Do not disclose whether username exists in detail to avoid user enumeration in some contexts;
                    # here we return a clear message but avoid leaking DB internals.
                    return jsonify({"error": "Username already taken"}), 409

                # Insert user record
                cur.execute(
                    "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                    (username, password_hash)
                )
                conn.commit()
        finally:
            try:
                conn.close()
            except Exception:
                logger.debug("Error closing DB connection", exc_info=True)

        # Successful registration: redirect to login (no user data echoed)
        return redirect('/login', code=302)

    except RuntimeError:
        # Configuration error - do not reveal internals
        return jsonify({"error": "Service temporarily unavailable"}), 503
    except MySQLdb.Error:
        # Generic DB error handling without exposing internals
        logger.exception("Database error during registration")
        return jsonify({"error": "Service temporarily unavailable"}), 500
    except Exception:
        # Catch-all: log minimal info and return generic error
        logger.exception("Unexpected error in registration")
        return jsonify({"error": "Service temporarily unavailable"}), 500
# ...existing code...
