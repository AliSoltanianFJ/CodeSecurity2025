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
    if not username or len(username) > 150 or not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "Invalid username.", 400

    # Validate and sanitize password input
    password = request.form.get('password', '').strip()
    if not password or len(password) < 8 or len(password) > 128:
        return "Invalid password.", 400

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    pepper = os.environ.get('PEPPER', '')
    if not pepper:
        return "Internal server error.", 500

    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt + pepper.encode('utf-8'),
        100_000
    )

    # Connect to DB securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'users'),
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password.hex(), salt.hex())
        )
        db.commit()
    except Exception as e:
        return "Registration failed.", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    return redirect('/login')