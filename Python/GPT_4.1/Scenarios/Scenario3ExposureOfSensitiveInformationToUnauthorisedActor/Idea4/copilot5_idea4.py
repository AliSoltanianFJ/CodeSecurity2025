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
    if len(username) > 150 or len(password) > 128:
        return "Invalid credentials", 400
    if not username.isalnum():
        return "Invalid credentials", 400

    # Principle of least privilege: use a dedicated DB user with minimal permissions
    # Do NOT hardcode credentials; use environment variables or a secrets manager
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_user, db_pass, db_name]):
        # Secure error handling: do not leak details
        return "Server configuration error", 500

    try:
        db = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            ssl_mode='REQUIRED'  # Ensure encrypted connection
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
        # Here, we use PBKDF2 via hashlib for demonstration
        password_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            stored_salt.encode('utf-8'),
            100_000  # Sufficient iterations for security
        ).hex()

        if password_hash != stored_hash:
            return "Invalid credentials", 401

        # At this point, authentication is successful
        # Implement session management securely (e.g., Flask-Login, secure cookies)
        # For demonstration, redirect to a secure page
        # Ensure HTTPS is enforced in production
        return redirect('/dashboard')

    except Exception:
        # Log error securely (not shown here), do not leak details to user
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass  # Avoid leaking errors

# ...existing code...