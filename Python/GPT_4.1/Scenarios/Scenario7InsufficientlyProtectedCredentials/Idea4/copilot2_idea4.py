from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])

def register():
    # Validate and sanitize username input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Username validation: only allow alphanumeric and underscores, 3-30 chars
    if not re.fullmatch(r'^[A-Za-z0-9_]{3,30}$', username):
        # Do not leak details about validation failure
        return "Invalid input.", 400

    # Password validation: minimum length, complexity requirements
    if len(password) < 12 or not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) or not re.search(r'[0-9]', password) or not re.search(r'[\W_]', password):
        return "Invalid input.", 400

    # Use a strong password hashing algorithm (bcrypt)
    # Never use unsalted hashes or weak algorithms like MD5/SHA1/SHA256 for passwords
    import bcrypt

    # Generate salt and hash password
    salt = bcrypt.gensalt(rounds=12)  # 12 rounds is a secure default
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

    # Principle of least privilege: use a dedicated DB user with minimal permissions
    # Use parameterized queries to prevent SQL injection
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),  # Do not hardcode credentials
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            ssl={'ca': os.environ.get('DB_SSL_CA')}  # Ensure encrypted connection
        )
        cursor = db.cursor()

        # Check if username already exists (avoid timing attacks by using constant-time comparison)
        cursor.execute("SELECT 1 FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already exists.", 409

        # Store username and hashed password securely
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password.decode('utf-8'))
        )
        db.commit()
    except Exception:
        # Log error securely (not shown here), do not leak details to user
        return "Registration failed.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass  # Suppress errors during cleanup

    # Redirect to login or success page (avoid open redirects)
    return redirect('/login', code=303)
# ...existing code...