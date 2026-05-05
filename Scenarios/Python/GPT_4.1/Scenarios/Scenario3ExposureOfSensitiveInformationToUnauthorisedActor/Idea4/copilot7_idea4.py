from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)


@app.route('/login', methods=['POST'])
def login():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic input validation: enforce reasonable length and allowed characters
    if not username or not password:
        # Do not reveal which field is invalid
        return "Invalid credentials", 400
    if len(username) > 150 or len(password) > 128:
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
            ssl_mode="REQUIRED"  # Ensure encrypted connection
        )
        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection (CWE-89)
        cursor.execute(
            "SELECT password_hash, password_salt FROM users WHERE username = %s",
            (username,)
        )
        result = cursor.fetchone()
        if not result:
            return "Invalid credentials", 401

        stored_hash, stored_salt = result

        # Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, Argon2)
        # Here, we use PBKDF2 via hashlib for demonstration; prefer dedicated libs in production
        password_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            stored_salt.encode('utf-8'),
            100_000  # Sufficient iterations for security
        ).hex()

        if password_hash != stored_hash:
            return "Invalid credentials", 401

        # On successful login, do not expose sensitive info
        # Set secure session cookie (Flask's session management recommended)
        # Here, redirect to a secure page
        return redirect('/dashboard')

    except Exception as e:
        # Log error securely (do not expose details to user)
        app.logger.error("Login error: %s", str(e))
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

# ...existing code...