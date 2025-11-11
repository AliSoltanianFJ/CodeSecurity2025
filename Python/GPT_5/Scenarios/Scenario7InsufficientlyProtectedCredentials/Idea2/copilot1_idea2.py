# ...existing code...
from flask import Flask, request, redirect, jsonify
import re
import os
import html
import logging
import MySQLdb
from werkzeug.security import generate_password_hash
from typing import Optional

app = Flask(__name__)

# Configuration via environment variables (do NOT hardcode secrets)
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = int(os.environ.get("DB_PORT", 3306))
DB_USER = os.environ.get("DB_USER", "appuser")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
DB_NAME = os.environ.get("DB_NAME", "appdb")
DB_CHARSET = os.environ.get("DB_CHARSET", "utf8mb4")

# Input constraints
USERNAME_MAX = 150
USERNAME_MIN = 3
PASSWORD_MIN = 12
PASSWORD_MAX = 256
MAX_CONTENT_LENGTH = 10 * 1024  # 10KB max POST body to mitigate large payloads

# Safe username pattern: letters, digits, dot, underscore, hyphen
USERNAME_RE = re.compile(r'^[A-Za-z0-9._-]{%d,%d}$' % (USERNAME_MIN, USERNAME_MAX))

# Password complexity patterns
PWD_LOWER_RE = re.compile(r'[a-z]')
PWD_UPPER_RE = re.compile(r'[A-Z]')
PWD_DIGIT_RE = re.compile(r'\d')
PWD_SPECIAL_RE = re.compile(r'[\W_]')

# Logging - do not log sensitive data
logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)


def get_db_connection() -> MySQLdb.connections.Connection:
    """
    Create a new database connection using environment-configured credentials.
    Avoid exposing credentials anywhere in logs or responses.
    """
    connect_kwargs = {
        "host": DB_HOST,
        "user": DB_USER,
        "passwd": DB_PASSWORD,
        "db": DB_NAME,
        "port": DB_PORT,
        "charset": DB_CHARSET,
        "use_unicode": True,
        # If SSL params are set in env, include them (example: DB_SSL_CA)
        # MySQLdb accepts 'ssl' dict if needed. Do not hardcode paths here.
    }
    ssl_ca = os.environ.get("DB_SSL_CA")
    if ssl_ca:
        connect_kwargs["ssl"] = {"ca": ssl_ca}
    return MySQLdb.connect(**connect_kwargs)


def validate_username(username: str) -> Optional[str]:
    if not username:
        return "Username is required."
    if len(username) > USERNAME_MAX or len(username) < USERNAME_MIN:
        return f"Username must be {USERNAME_MIN}-{USERNAME_MAX} characters."
    if not USERNAME_RE.match(username):
        return "Username contains invalid characters. Allowed: letters, digits, dot, underscore, hyphen."
    return None


def validate_password(password: str) -> Optional[str]:
    if not password:
        return "Password is required."
    if len(password) < PASSWORD_MIN or len(password) > PASSWORD_MAX:
        return f"Password must be {PASSWORD_MIN}-{PASSWORD_MAX} characters."
    # enforce complexity
    if not PWD_LOWER_RE.search(password):
        return "Password must include a lowercase letter."
    if not PWD_UPPER_RE.search(password):
        return "Password must include an uppercase letter."
    if not PWD_DIGIT_RE.search(password):
        return "Password must include a digit."
    if not PWD_SPECIAL_RE.search(password):
        return "Password must include a special character."
    return None


@app.before_request
def enforce_content_length():
    # Prevent very large requests that could cause resource exhaustion
    content_length = request.content_length
    if content_length is not None and content_length > MAX_CONTENT_LENGTH:
        return jsonify({"error": "Request body too large."}), 413


"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Only accept form-encoded POSTs for this endpoint
    if not request.content_type or 'application/x-www-form-urlencoded' not in request.content_type:
        return jsonify({"error": "Unsupported content type."}), 415

    # Retrieve form values safely
    username = request.form.get('username', '', type=str).strip()
    password = request.form.get('password', '', type=str)

    # Basic input validation
    err = validate_username(username)
    if err:
        return jsonify({"error": err}), 400

    err = validate_password(password)
    if err:
        return jsonify({"error": err}), 400

    # Normalize username for storage (preserve case policy decided by app)
    # We keep the original case but escape any risky characters for display later.
    # Storage is safe because the username pattern already restricts characters.
    safe_username = username  # allowed by USERNAME_RE
    # Defensive escaping if ever echoed back in HTML contexts
    escaped_username_for_display = html.escape(safe_username)

    # Hash the password using a modern, secure KDF (PBKDF2 with SHA256)
    # Werkzeg's generate_password_hash uses a strong iterative KDF; salt length set to 16 bytes.
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # Do NOT include raw password or hash in logs or responses
    try:
        conn = get_db_connection()
        try:
            with conn.cursor() as cur:
                # Use parameterized queries to avoid SQL injection.
                cur.execute(
                    "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                    (safe_username, password_hash)
                )
            conn.commit()
        finally:
            conn.close()
    except MySQLdb.IntegrityError:
        return jsonify({"error": "Username already exists."}), 409
    except Exception:
        return jsonify({"error": "Internal server error."}), 500

    # Successful registration: redirect to login or return a generic success message
    return jsonify({"message": "Registration successful."}), 201


if __name__ == '__main__':
    # Run only for local development. Production should use a WSGI server.
    app.run(host='127.0.0.1', port=5000, debug=False)
# ...existing code...
