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

    # Input validation: only allow alphanumeric usernames, length check
    if not re.match(r'^[A-Za-z0-9_]{3,32}$', username):
        return "Invalid username.", 400
    if len(password) < 8 or len(password) > 128:
        return "Password must be between 8 and 128 characters.", 400

    # Sanitize username (already validated, but double check)
    username = re.sub(r'[^\w]', '', username)

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    hashed_pw = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )
    # Store salt and hash as hex
    salt_hex = salt.hex()
    hash_hex = hashed_pw.hex()

    # Connect to DB securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'usersdb'),
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

    return redirect('/login')
# ...existing code...