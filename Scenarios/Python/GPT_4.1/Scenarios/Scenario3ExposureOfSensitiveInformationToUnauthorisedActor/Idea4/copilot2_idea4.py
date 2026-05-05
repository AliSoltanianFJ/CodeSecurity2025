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
            # Do not reveal if username exists
            return "Invalid credentials", 401

        stored_hash, stored_salt = result

        # Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, Argon2)
        # Here, we use PBKDF2 with SHA-256
        password_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            stored_salt.encode('utf-8'),
            100_000  # Iterations
        ).hex()

        if password_hash != stored_hash:
            return "Invalid credentials", 401

        # Successful login: set session securely (Flask session should use secure cookies)
        # Do not expose sensitive info in response
        # Consider using Flask-Login for session management and CSRF protection

        # Example: Set session (ensure SECRET_KEY is set and secure)
        from flask import session
        session['username'] = username

        return redirect('/dashboard')

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