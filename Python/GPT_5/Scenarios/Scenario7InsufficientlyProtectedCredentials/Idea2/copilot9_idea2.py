# ...existing code...
from flask import Flask, request, redirect, jsonify, abort
import re
import os
import logging
import MySQLdb
from werkzeug.security import generate_password_hash
from markupsafe import escape

app = Flask(__name__)

# Basic logger (does not expose secrets)
logger = logging.getLogger(__name__)
if not logger.handlers:
    logging.basicConfig(level=logging.INFO)

# Validation rules
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_.-]{3,150}$')  # allowed characters and length
MIN_PASSWORD_LENGTH = 12
MAX_PASSWORD_LENGTH = 1024  # prevent extremely large inputs that could cause resource issues
PASSWORD_COMPLEXITY_REGEX = re.compile(
    r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).+$'
)  # lower, upper, digit, special

def _validate_username(username: str) -> bool:
    if not username:
        return False
    # Reject control characters
    if any(ord(c) < 32 for c in username):
        return False
    return bool(USERNAME_REGEX.fullmatch(username))

def _validate_password(password: str) -> bool:
    if not password:
        return False
    if not (MIN_PASSWORD_LENGTH <= len(password) <= MAX_PASSWORD_LENGTH):
        return False
    # Complexity: at least one lowercase, uppercase, digit and special char
    return bool(PASSWORD_COMPLEXITY_REGEX.match(password))

def _get_db_connection():
    # Use environment variables for credentials; fail fast without printing secrets
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASSWORD')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_pass, db_name]):
        logger.error("Database connection environment variables are not fully set.")
        raise RuntimeError("Server configuration error")

    conn = MySQLdb.connect(
        host=db_host,
        user=db_user,
        passwd=db_pass,
        db=db_name,
        charset='utf8mb4',
        use_unicode=True,
        connect_timeout=5
    )
    return conn

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Retrieve form fields safely
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic input validation and sanitation
    if not _validate_username(username) or not _validate_password(password):
        # Do not leak which field failed; generic message
        return jsonify({"error": "Invalid input"}), 400

    # Hash password using a current PBKDF2 scheme (werkzeug generates a salted hash)
    try:
        password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    except Exception:
        logger.exception("Password hashing failed.")
        return jsonify({"error": "Server error"}), 500

    # Store user with parameterized queries to prevent SQL injection
    try:
        conn = _get_db_connection()
        try:
            cur = conn.cursor()
            # Ensure the users table uses a UNIQUE constraint on username in the DB schema.
            # Parameterized query prevents injection.
            insert_sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
            cur.execute(insert_sql, (username, password_hash))
            conn.commit()
        finally:
            try:
                cur.close()
            except Exception:
                pass
            try:
                conn.close()
            except Exception:
                pass
    except MySQLdb.IntegrityError:
        # Likely a duplicate username (assumes UNIQUE constraint). Generic response to avoid info leakage.
        logger.info("Attempt to register duplicate username.")
        return jsonify({"error": "Registration failed"}), 409
    except RuntimeError:
        # Configuration issue already logged
        return jsonify({"error": "Server configuration error"}), 500
    except Exception:
        logger.exception("Unexpected error during registration.")
        return jsonify({"error": "Server error"}), 500

    # Successful registration; do not echo back PII (username) in the response to avoid information leakage.
    # Redirect to login page (do not include query params with sensitive data).
    return redirect("/login", code=303)
# ...existing code...
