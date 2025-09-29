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

    try:
        # Use least privilege: create a dedicated DB user with only SELECT privilege for authentication
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "readonly_user"),
            passwd=os.environ.get("DB_PASS", ""),
            db=os.environ.get("DB_NAME", "db"),
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
            # Do not reveal if username exists
            return "Invalid credentials", 401

        stored_hash, stored_salt = result

        # Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, Argon2)
        # Here, we use PBKDF2 with SHA-256
        import base64
        import hmac

        # Decode salt if stored as base64
        salt = base64.b64decode(stored_salt)
        dk = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt,
            100_000  # Iteration count; tune as needed
        )
        computed_hash = base64.b64encode(dk).decode('utf-8')

        # Use constant-time comparison to prevent timing attacks (CWE-203)
        if hmac.compare_digest(computed_hash, stored_hash):
            # Authentication successful
            # Implement session management securely (e.g., using Flask-Login, with secure cookies)
            # For demonstration, redirect to a secure page
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401

    except Exception as e:
        # Log error securely (do not expose details to user)
        app.logger.error("Authentication error: %s", str(e))
        return "Authentication failed", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

# ...existing code...