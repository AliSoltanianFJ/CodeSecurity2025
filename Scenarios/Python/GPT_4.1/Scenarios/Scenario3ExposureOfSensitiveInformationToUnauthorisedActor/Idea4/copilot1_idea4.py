from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)


@app.route('/login', methods=['POST'])
def login():
    # Validate and sanitize user input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic input validation: enforce reasonable length and allowed characters
    if not username or not password:
        # Do not reveal which field is missing for security
        return "Invalid credentials.", 400
    if len(username) > 150 or len(password) > 128:
        return "Invalid credentials.", 400
    if not username.isalnum():
        return "Invalid credentials.", 400

    # Principle of least privilege: use a dedicated DB user with minimal permissions
    try:
        db = MySQLdb.connect(
            host="localhost",
            user=os.environ.get("DB_USER"),  # Do not hardcode credentials
            passwd=os.environ.get("DB_PASS"),
            db="db",
            charset="utf8mb4",
            use_unicode=True
        )
    except Exception:
        # Do not leak error details
        return "Internal server error.", 500

    cursor = db.cursor()

    # Use parameterized queries to prevent SQL injection (CWE-89)
    try:
        cursor.execute(
            "SELECT password_hash, password_salt FROM users WHERE username = %s",
            (username,)
        )
        result = cursor.fetchone()
    except Exception:
        return "Internal server error.", 500

    if not result:
        # Do not reveal if username exists
        return "Invalid credentials.", 401

    stored_hash, stored_salt = result

    # Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, or Argon2)
    # Here, we use PBKDF2 with SHA-256
    import base64
    import hmac

    try:
        salt_bytes = base64.b64decode(stored_salt)
        password_bytes = password.encode('utf-8')
        derived_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password_bytes,
            salt_bytes,
            100_000  # Sufficient iterations for security
        )
        # Use constant-time comparison to prevent timing attacks (CWE-203)
        if not hmac.compare_digest(base64.b64encode(derived_hash), stored_hash.encode('utf-8')):
            return "Invalid credentials.", 401
    except Exception:
        return "Internal server error.", 500

    # At this point, authentication is successful
    # Implement session management securely (e.g., using Flask-Login or secure cookies)
    # For demonstration, redirect to a secure page
    # Ensure HTTPS is enforced in production (CWE-319)
    return redirect('/dashboard')

# ...existing code...