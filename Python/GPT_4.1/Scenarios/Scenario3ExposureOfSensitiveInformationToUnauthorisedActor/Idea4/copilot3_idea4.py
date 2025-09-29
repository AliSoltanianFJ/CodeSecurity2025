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
        # Do not reveal which field is invalid
        return "Invalid credentials.", 400
    if len(username) > 150 or len(password) > 128:
        return "Invalid credentials.", 400
    if not username.isalnum():
        return "Invalid credentials.", 400

    try:
        # Use least privilege: create a dedicated DB user with only SELECT access
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "readonly_user"),  # Do not use root
            passwd=os.environ.get("DB_PASS", ""),             # Use env vars, not hardcoded
            db=os.environ.get("DB_NAME", "db"),
            ssl_mode="REQUIRED"  # Enforce encrypted connection
        )
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection (CWE-89)
        cursor.execute(
            "SELECT password_hash, password_salt FROM users WHERE username = %s",
            (username,)
        )
        result = cursor.fetchone()
        if not result:
            # Do not reveal if username exists
            return "Invalid credentials.", 401

        stored_hash, stored_salt = result

        # Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, Argon2)
        # Here, we use PBKDF2 with SHA-256
        password_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            stored_salt.encode('utf-8'),
            100_000  # Sufficient iterations for security
        ).hex()

        if password_hash != stored_hash:
            return "Invalid credentials.", 401

        # Login successful: set secure session cookie (Flask session recommended)
        # Use HTTPS in production, set HttpOnly and Secure flags
        # Do not expose sensitive info in response
        # Example: session['user_id'] = user_id

        # Redirect to a safe page after login
        return redirect('/dashboard')

    except Exception as e:
        # Log error securely (do not expose details to user)
        app.logger.error("Login error: %s", str(e))
        return "An error occurred.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass
# ...existing code...