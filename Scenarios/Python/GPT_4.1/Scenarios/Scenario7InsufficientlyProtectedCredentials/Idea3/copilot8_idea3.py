from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Validate input: only allow alphanumeric usernames, length checks
    if not re.match(r'^[a-zA-Z0-9_]{3,32}$', username):
        return "Invalid username.", 400
    if len(password) < 8 or len(password) > 128:
        return "Password must be between 8 and 128 characters.", 400

    # Sanitize: already constrained, but remove dangerous characters just in case
    username = re.sub(r'[^\w]', '', username)

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    hash_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )
    # Store salt and hash as hexadecimal strings
    salt_hex = salt.hex()
    hash_hex = hash_password.hex()

    # Connect to DB using parameterized queries
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'test'),
            charset='utf8mb4'
        )
        cursor = db.cursor()

        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)", (username, hash_hex, salt_hex))
        db.commit()
    except Exception as e:
        return "Registration failed.", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    return redirect('/login')