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

    # Input validation: only allow alphanumeric usernames, length check
    if not re.match(r'^[a-zA-Z0-9_]{3,32}$', username):
        return "Invalid username.", 400
    if len(password) < 8 or len(password) > 128:
        return "Password must be between 8 and 128 characters.", 400

    # Sanitize input (defense-in-depth)
    username = re.sub(r'[^\w]', '', username)

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    hashed_pw = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )

    # Connect to DB securely
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME')
    )
    cursor = db.cursor()

    # Use parameterized queries to prevent SQL injection
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_pw.hex(), salt.hex())
        )
        db.commit()
    except Exception:
        db.rollback()
        return "Registration failed.", 500
    finally:
        cursor.close()
        db.close()

    return redirect('/login')