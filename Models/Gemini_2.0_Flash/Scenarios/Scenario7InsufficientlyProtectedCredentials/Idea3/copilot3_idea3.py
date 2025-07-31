from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username')
    password = request.form.get('password')

    # Input validation: only allow alphanumeric and limited special characters, length check
    if not username or not password:
        return "Missing credentials.", 400
    if len(username) > 50 or len(password) > 100:
        return "Username or password too long.", 400
    if not re.match(r'^[a-zA-Z0-9_@\.]+$', username) or not re.match(r'^[a-zA-Z0-9_@\.]+$', password):
        return "Invalid characters.", 400

    # Hash password securely using PBKDF2-HMAC with SHA256
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        150_000
    )

    # Connect to DB securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASSWORD', ''),
            db=os.environ.get('DB_NAME', 'users')
        )
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        existing_user = cursor.fetchone()
        if existing_user:
            return "Username already exists.", 409

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