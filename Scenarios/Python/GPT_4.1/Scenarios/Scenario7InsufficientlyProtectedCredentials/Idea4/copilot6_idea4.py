from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""

# ...existing code...

@app.route('/register', methods=['POST'])
def register():
    # Security: Validate and sanitize all user inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Username validation: allow only alphanumeric and underscores, 3-30 chars
    if not re.fullmatch(r'^[A-Za-z0-9_]{3,30}$', username):
        # Do not leak details about validation failure
        return "Invalid input.", 400

    # Password validation: minimum length, complexity requirements
    if len(password) < 12 or not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) or not re.search(r'[0-9]', password) or not re.search(r'[\W_]', password):
        return "Invalid input.", 400

    # Security: Use a strong password hashing algorithm (bcrypt)
    # Do NOT use hashlib for password hashing (CWE-916)
    import bcrypt

    # Generate a cryptographically secure salt and hash the password
    salt = bcrypt.gensalt(rounds=12)  # rounds=12 is a secure default
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

    # Security: Use parameterized queries to prevent SQL injection (CWE-89)
    # Principle of least privilege: Use a DB user with minimal permissions
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),  # Do NOT hardcode credentials
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            ssl_mode='REQUIRED'  # Secure connection
        )
        cursor = db.cursor()

        # Insert user securely
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password.decode('utf-8'))
        )
        db.commit()
    except Exception as e:
        # Proper error handling: log error securely, do not leak details to user
        import logging
        logging.error("Registration error: %s", str(e))
        return "Registration failed.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    # Security: Redirect to login page or show generic success message
    return "Registration successful.", 201

# ...existing code...