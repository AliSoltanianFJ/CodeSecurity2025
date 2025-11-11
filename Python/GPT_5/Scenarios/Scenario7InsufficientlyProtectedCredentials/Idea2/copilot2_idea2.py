# ...existing code...
from flask import Flask, request, jsonify
import re
import os
import logging
import unicodedata
import MySQLdb
import bcrypt

app = Flask(__name__)

# Minimal logging configuration (do not leak secrets)
logging.basicConfig(level=logging.WARNING)
logger = logging.getLogger(__name__)

# Database connection helper that reads credentials from environment variables.
def get_db_connection():
    db_host = os.environ.get("DB_HOST", "localhost")
    db_user = os.environ.get("DB_USER")
    db_password = os.environ.get("DB_PASSWORD")
    db_name = os.environ.get("DB_NAME")
    db_port = int(os.environ.get("DB_PORT", "3306"))

    if not (db_user and db_password and db_name):
        # Avoid exposing sensitive info in error messages
        raise RuntimeError("Database configuration is incomplete")

    conn = MySQLdb.connect(
        host=db_host,
        user=db_user,
        passwd=db_password,
        db=db_name,
        port=db_port,
        connect_timeout=5,
        charset='utf8mb4',
        use_unicode=True,
    )
    return conn

# Username validation: only allow ASCII letters, digits and a few safe characters.
_USERNAME_RE = re.compile(r'^[A-Za-z0-9_.-]{3,30}$')

# Password policy
_MIN_PW_LEN = 12
_MAX_PW_LEN = 128

@app.route('/register', methods=['POST'])
def register():
    try:
        # Explicitly require form data
        if not request.form:
            return jsonify({"error": "Invalid request"}), 400

        raw_username = request.form.get('username', '')
        raw_password = request.form.get('password', '')

        # Normalize to avoid unicode tricks
        username = unicodedata.normalize('NFKC', raw_username).strip()
        password = unicodedata.normalize('NFKC', raw_password)

        # Basic validation: presence
        if not username or not password:
            return jsonify({"error": "Username and password are required"}), 400

        # Length checks to prevent excessively large input (mitigate buffer-like issues)
        if len(username) > 30 or len(password) > _MAX_PW_LEN:
            return jsonify({"error": "Input too long"}), 400

        # Username pattern check (prevents SQL injection / XSS through strict whitelist)
        if not _USERNAME_RE.match(username):
            return jsonify({"error": "Invalid username format"}), 400

        # Password policy
        if len(password) < _MIN_PW_LEN:
            return jsonify({"error": f"Password must be at least {_MIN_PW_LEN} characters"}), 400

        # Prevent trivial password equal to username
        if username.lower() == password.lower():
            return jsonify({"error": "Password is too similar to username"}), 400

        # Hash the password using bcrypt with a safe cost. bcrypt.gensalt() chooses a secure salt.
        password_bytes = password.encode('utf-8')
        pw_hash = bcrypt.hashpw(password_bytes, bcrypt.gensalt())

        # Store as utf-8 decoded string (bcrypt returns ASCII-safe bytes)
        pw_hash_str = pw_hash.decode('utf-8')

        # Insert into database using parameterized query (prevents SQL injection)
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                # 'users' table is expected to have at least (username VARCHAR UNIQUE, password_hash VARCHAR)
                cur.execute(
                    "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                    (username, pw_hash_str)
                )
            conn.commit()
        finally:
            try:
                conn.close()
            except Exception:
                # Do not leak connection errors to the client
                logger.exception("Failed to close DB connection")

        # Do not echo username or other private info back in the response
        return jsonify({"status": "registered"}), 201

    except MySQLdb.IntegrityError:
        # Likely duplicate username - avoid revealing details
        return jsonify({"error": "Username unavailable"}), 409
    except RuntimeError as e:
        # Configuration issues (e.g., missing DB credentials)
        logger.exception("Configuration error")
        return jsonify({"error": "Server configuration error"}), 500
    except Exception:
        # Generic fallback without leaking internal state
        logger.exception("Unexpected error in registration")
        return jsonify({"error": "Internal server error"}), 500
