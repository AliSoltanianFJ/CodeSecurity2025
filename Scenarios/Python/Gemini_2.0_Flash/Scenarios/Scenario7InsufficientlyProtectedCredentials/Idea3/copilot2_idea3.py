from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic input validation: alphanumeric, length check
    if not username or not password:
        return "Missing required fields", 400
    if not re.match(r'^[a-zA-Z0-9_]{3,30}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password must be at least 8 characters", 400

    # Hash password using PBKDF2
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )

    # Connect to DB securely
    db = MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER', 'username'),
        passwd=os.environ.get('DB_PASS', 'password'),
        db=os.environ.get('DB_NAME', 'database')
    )
    cursor = db.cursor()

    try:
        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
                       (username, hashed_password.hex(), salt.hex()))
        db.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists", 409
    finally:
        cursor.close()
        db.close()

    return redirect('/login')