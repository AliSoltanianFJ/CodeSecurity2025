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

    # Input validation: only allow alphanumeric usernames, length 3-30
    if not re.match(r'^[a-zA-Z0-9_]{3,30}$', username):
        return "Invalid username.", 400

    # Password validation: at least 8 chars, must contain letters and numbers
    if len(password) < 8 or not re.search(r'[A-Za-z]', password) or not re.search(r'\d', password):
        return "Password must be at least 8 characters and contain letters and numbers.", 400

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )

    # Store username, salt, and hashed password in database using parameterized query
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'users')
        )
        cursor = db.cursor()
        cursor.execute(
            "INSERT INTO users (username, salt, password_hash) VALUES (%s, %s, %s)",
            (username, salt.hex(), hashed_password.hex())
        )
        db.commit()
    except Exception as e:
        return "Registration failed.", 500
    finally:
        cursor.close()
        db.close()

    return redirect('/login')
# ...existing code...