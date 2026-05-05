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
    password = request.form.get('password', '').strip()

    # Basic input validation: enforce reasonable length and allowed characters
    if not username or not password:
        # Do not reveal which field is missing
        return "Invalid credentials", 400
    if len(username) > 64 or len(password) > 128:
        return "Invalid credentials", 400
    if not username.isalnum():
        return "Invalid credentials", 400

    # Principle of least privilege: use a dedicated DB user with minimal permissions
    try:
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER"),  # Do not hardcode credentials
            passwd=os.environ.get("DB_PASS"),
            db=os.environ.get("DB_NAME"),
            ssl_mode="REQUIRED"  # Enforce encrypted connection
        )
    except Exception:
        # Do not leak error details
        return "Internal server error", 500

    cursor = db.cursor()

    # Use parameterized queries to prevent SQL injection (CWE-89)
    try:
        cursor.execute(
            "SELECT password_hash, password_salt FROM users WHERE username = %s",
            (username,)
        )
        result = cursor.fetchone()
    except Exception:
        return "Internal server error", 500

    if not result:
        # Do not reveal if username exists
        return "Invalid credentials", 401

    stored_hash, stored_salt = result

    # Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, Argon2)
    # Here, we use PBKDF2 with SHA-256
    def hash_password(password, salt):
        return hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt.encode('utf-8'),
            100_000  # OWASP recommends >=100,000 iterations
        ).hex()

    computed_hash = hash_password(password, stored_salt)

    # Constant-time comparison to prevent timing attacks (CWE-203)
    if not hashlib.compare_digest(computed_hash, stored_hash):
        return "Invalid credentials", 401

    # At this point, authentication is successful
    # Implement session management securely (e.g., using Flask-Login, secure cookies)
    # For demonstration, redirect to a secure page
    # Set secure cookie attributes: HttpOnly, Secure, SameSite
    response = redirect('/dashboard')
    response.set_cookie(
        'session_id',
        value=os.urandom(32).hex(),  # Use a cryptographically secure random session ID
        httponly=True,
        secure=True,  # Only send cookie over HTTPS
        samesite='Strict'
    )
    return response

# ...existing code...