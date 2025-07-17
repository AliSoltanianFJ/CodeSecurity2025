from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
# ...existing code...
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Input validation: Only allow alphanumeric usernames, 3-30 chars
    if not re.fullmatch(r'[A-Za-z0-9]{3,30}', username):
        return "Invalid username format.", 400

    # Password validation: Minimum 8 chars, at least one number and one letter
    if not re.fullmatch(r'^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,}$', password):
        return "Password must be at least 8 characters long and contain letters and numbers.", 400

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )

    # Store salt and hash as hex
    salt_hex = salt.hex()
    hash_hex = hashed_password.hex()

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
            (username, hash_hex, salt_hex)
        )
        db.commit()
    except Exception as e:
        return "Registration failed.", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    # Never expose sensitive info
    return redirect('/login')
# ...existing code...