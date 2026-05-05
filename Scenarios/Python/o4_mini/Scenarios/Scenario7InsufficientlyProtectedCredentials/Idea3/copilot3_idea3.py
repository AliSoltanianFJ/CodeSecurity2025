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

    # 1. Basic validation
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Username must be 3–30 chars: letters, numbers, underscores", 400
    if len(password) < 8:
        return "Password must be at least 8 characters", 400

    # 2. Connect to the database
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'appuser'),
            passwd=os.getenv('DB_PASS', 'secret'),
            db=os.getenv('DB_NAME', 'appdb'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
    except MySQLdb.Error:
        return "Database connection error", 500

    # 3. Check for existing user
    cursor.execute("SELECT 1 FROM users WHERE username=%s", (username,))
    if cursor.fetchone():
        cursor.close()
        db.close()
        return "Username already taken", 409

    # 4. Generate salt + hash
    salt = os.urandom(16)       # 16 bytes = 128 bits
    # pbkdf2_hmac returns bytes
    dk = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000                   # iterations
    )
    salt_hex = salt.hex()
    hash_hex = dk.hex()

    # 5. Insert new user
    try:
        cursor.execute(
            "INSERT INTO users (username, salt, pwhash) VALUES (%s, %s, %s)",
            (username, salt_hex, hash_hex)
        )
        db.commit()
    except MySQLdb.Error:
        db.rollback()
        cursor.close()
        db.close()
        return "Registration failed", 500

    cursor.close()
    db.close()

    # 6. Redirect or success message
    return redirect('/login')