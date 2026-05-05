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
        # Do not reveal which field is missing
        return "Invalid credentials", 400
    if len(username) > 150 or len(password) > 128:
        return "Invalid credentials", 400
    if not username.isalnum():
        return "Invalid credentials", 400

    try:
        # Use least privilege: create a dedicated DB user with only SELECT privilege for authentication
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "readonly_user"),  # Do not use root
            passwd=os.environ.get("DB_PASS", ""),             # Do not hardcode secrets
            db=os.environ.get("DB_NAME", "db"),
            ssl_mode="REQUIRED"                               # Enforce encrypted connection
        )
        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection (CWE-89)
        cursor.execute(
            "SELECT password_hash, password_salt FROM users WHERE username = %s",
            (username,)
        )
        result = cursor.fetchone()
        if not result:
            # Do not reveal if username exists
            return "Invalid credentials", 401

        stored_hash, stored_salt = result

        # Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, Argon2)
        # Here, we use PBKDF2 with SHA-256
        password_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            stored_salt.encode('utf-8'),
            200_000  # High iteration count for security
        ).hex()

        if password_hash != stored_hash:
            return "Invalid credentials", 401

        # Authentication successful
        # Generate a secure session token (do not use predictable values)
        import secrets
        session_token = secrets.token_urlsafe(32)

        # Store session token securely (e.g., in a secure session store)
        # For demonstration, set a secure cookie
        response = redirect('/dashboard')
        response.set_cookie(
            'session_token',
            session_token,
            httponly=True,      # Mitigate XSS
            secure=True,        # Only send over HTTPS
            samesite='Strict'   # Mitigate CSRF
        )
        return response

    except Exception as e:
        # Log error securely (do not leak details to user)
        import logging
        logging.error("Login error: %s", str(e))
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

# ...existing code...