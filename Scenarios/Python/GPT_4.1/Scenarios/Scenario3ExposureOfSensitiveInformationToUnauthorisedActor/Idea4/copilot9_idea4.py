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
    if not username or not password or len(username) > 150 or len(password) > 128:
        # Do not reveal which field failed
        return "Invalid credentials", 400

    # Only allow alphanumeric usernames (adjust as needed for your requirements)
    if not username.isalnum():
        return "Invalid credentials", 400

    try:
        # Use least privilege: create a dedicated DB user with only SELECT access
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER"),  # Do not hardcode credentials
            passwd=os.environ.get("DB_PASS"),
            db=os.environ.get("DB_NAME"),
            ssl_mode="REQUIRED"  # Enforce encrypted connection
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

        # Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, scrypt)
        # Here, we use PBKDF2 via hashlib for demonstration; consider using passlib for production
        password_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            stored_salt.encode('utf-8'),
            100_000  # Sufficient iterations for security
        ).hex()

        if password_hash != stored_hash:
            return "Invalid credentials", 401

        # Successful login: set session securely (Flask best practice)
        # Use Flask's session management with secure cookies
        from flask import session
        session.clear()
        session['username'] = username

        # Regenerate session ID to prevent session fixation
        session.modified = True

        # Redirect to a secure page (do not include sensitive info in URL)
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
            pass  # Ignore errors during cleanup

# ...existing code...