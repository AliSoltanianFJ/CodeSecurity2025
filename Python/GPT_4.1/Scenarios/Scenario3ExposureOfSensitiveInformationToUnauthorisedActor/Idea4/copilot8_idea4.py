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
        # Do not reveal which field is invalid
        return "Invalid credentials", 400
    if len(username) > 150 or len(password) > 128:
        return "Invalid credentials", 400
    if not username.isalnum():
        return "Invalid credentials", 400

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
        # Here, we use hashlib.pbkdf2_hmac for demonstration; consider using passlib for production
        password_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            stored_salt.encode('utf-8'),
            100_000
        ).hex()

        if password_hash != stored_hash:
            return "Invalid credentials", 401

        # On successful login, set a secure session cookie (Flask's session management)
        # Ensure session cookies are set with secure, HttpOnly, and SameSite flags
        # (Configure Flask app accordingly elsewhere)
        # Do not expose sensitive info in responses
        return redirect('/dashboard')

    except Exception as e:
        # Log error securely (do not leak details to user)
        app.logger.error("Login error: %s", str(e))
        return "An error occurred", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

# ...existing code...