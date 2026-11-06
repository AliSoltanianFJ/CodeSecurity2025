from flask import Flask, request, redirect, abort
from werkzeug.security import generate_password_hash
import re
import MySQLdb
import os
from markupsafe import escape

app = Flask(__name__)

# Security Decision: Ensure secret key is not hardcoded, use environment variable or secure config management
app.secret_key = os.environ.get('FLASK_SECRET_KEY', os.urandom(32))

# Security Decision: Only allow valid, safe characters in username; enforce length restrictions
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_.-]{3,30}$')  # Only allow alphanumeric and safe characters, length 3-30

# Security Decision: Enforce strong password policy (min length, complexity)
PASSWORD_REGEX = re.compile(r'^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[!@#$%^&*()_+=-])[A-Za-z\d!@#$%^&*()_+=-]{12,128}$')

# Security Decision: Never hardcode DB credentials; use environment variables
DB_CONFIG = {
    "host": os.environ.get("DB_HOST", "localhost"),
    "user": os.environ.get("DB_USER"),
    "passwd": os.environ.get("DB_PASS"),
    "db": os.environ.get("DB_NAME"),
    "port": int(os.environ.get("DB_PORT", 3306)),
    "ssl": {"ca": os.environ.get("DB_SSL_CA")} if os.environ.get("DB_SSL_CA") else None,  # Enforce SSL if CA provided
    "use_unicode": True,
    "charset": "utf8mb4"
}

@app.route('/register', methods=['POST'])
def register():
    # SECURITY: Enforce HTTPS
    if not request.is_secure:
        abort(403)  # Never serve registration over insecure connection

    # SECURITY: Validate and sanitize all input
    username = request.form.get('username', '', type=str).strip()
    password = request.form.get('password', '', type=str)

    # SECURITY: Strict input validation
    if not USERNAME_REGEX.fullmatch(username):
        abort(400, description="Invalid username format.")
    if not PASSWORD_REGEX.fullmatch(password):
        abort(400, description="Password does not meet complexity requirements.")

    # SECURITY: Prevent XSS by escaping output (if ever reflected)
    safe_username = escape(username)

    # SECURITY: Use strong password hashing (PBKDF2, bcrypt, or argon2); never store plaintext or unsalted hash
    password_hash = generate_password_hash(password, method="pbkdf2:sha256", salt_length=16)

    # SECURITY: Least privilege DB access (account must have only required permissions)
    try:
        conn = MySQLdb.connect(**DB_CONFIG)
        cursor = conn.cursor()
        # SECURITY: Use parameterized queries to prevent SQL injection (CWE-89)
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (safe_username, password_hash)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        # SECURITY: Do not leak details (e.g., about whether username exists)
        abort(409, description="Registration error.")
    except Exception:
        abort(500, description="Internal server error.")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass  # Never leak errors

    # SECURITY: Do not expose sensitive info in response
    return redirect('/login')

# SECURITY: Ensure all responses have appropriate security headers
@app.after_request
def set_secure_headers(response):
    response.headers["Content-Security-Policy"] = "default-src 'none';"
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Referrer-Policy"] = "no-referrer"
    return response
