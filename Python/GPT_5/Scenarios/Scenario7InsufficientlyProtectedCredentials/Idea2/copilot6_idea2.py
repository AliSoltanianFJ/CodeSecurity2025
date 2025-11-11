from flask import Flask, request, jsonify, after_this_request
from werkzeug.security import generate_password_hash
import re
import os
import logging
import MySQLdb
from MySQLdb import OperationalError, IntegrityError

app = Flask(__name__)

# Configure logging (do not expose sensitive details in responses)
logging.basicConfig(level=logging.INFO)

# Load DB credentials from environment variables
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER", "appuser")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
DB_NAME = os.environ.get("DB_NAME", "appdb")

# Security headers for all responses
@app.after_request
def set_secure_headers(response):
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "no-referrer"
    # Minimal CSP to reduce XSS risk; adjust as needed for your app
    response.headers["Content-Security-Policy"] = "default-src 'none'; frame-ancestors 'none';"
    return response

# Strong username regex: allows letters, numbers, underscore, dot and hyphen
USERNAME_RE = re.compile(r'^[A-Za-z0-9_.-]{3,32}$')

# Basic password policy
MIN_PASSWORD_LENGTH = 12
MAX_PASSWORD_LENGTH = 256  # protect against excessive input sizes

def _get_db_connection():
    # Use UTF-8 and explicit unicode handling
    return MySQLdb.connect(
        host=DB_HOST,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        charset='utf8mb4',
        use_unicode=True,
        connect_timeout=5
    )

@app.route('/register', methods=['POST'])
def register():
    # Only accept form-encoded or JSON; avoid accepting arbitrary content types
    username = request.form.get('username', '') if request.mimetype == 'application/x-www-form-urlencoded' else request.json.get('username') if request.is_json else ''
    password = request.form.get('password', '') if request.mimetype == 'application/x-www-form-urlencoded' else request.json.get('password') if request.is_json else ''

    # Normalize and validate presence
    if not isinstance(username, str) or not isinstance(password, str):
        return jsonify({"error": "Invalid input"}), 400

    username = username.strip()

    # Validate username
    if not USERNAME_RE.match(username):
        return jsonify({"error": "Invalid username"}), 400

    # Validate password length and control characters
    if not (MIN_PASSWORD_LENGTH <= len(password) <= MAX_PASSWORD_LENGTH):
        return jsonify({"error": "Invalid password"}), 400
    if any(ord(ch) < 32 for ch in password):  # disallow control chars
        return jsonify({"error": "Invalid password"}), 400

    # Hash password using PBKDF2 with SHA-256 (werkzeug's generate_password_hash)
    # Salt length default provided; iterations are adequate for PBKDF2 in werkzeug
    try:
        password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    except Exception:
        logging.exception("Password hashing failed")
        return jsonify({"error": "Internal error"}), 500

    # Store in database using parameterized query to prevent SQL injection
    try:
        conn = _get_db_connection()
        try:
            with conn.cursor() as cursor:
                # Ensure the users table has appropriate constraints (unique username)
                sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
                cursor.execute(sql, (username, password_hash))
            conn.commit()
        finally:
            conn.close()
    except IntegrityError as e:
        return jsonify({"error": "User already exists"}), 409
    except OperationalError:
        logging.exception("Database operational error during registration")
        return jsonify({"error": "Service unavailable"}), 503
    except Exception:
        logging.exception("Unexpected error during registration")
        return jsonify({"error": "Internal error"}), 500

    # Successful creation - do not return sensitive info
    return jsonify({"status": "created"}), 201