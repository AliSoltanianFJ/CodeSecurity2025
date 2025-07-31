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

    # Validate input: alphanumeric, length
    if not username.isalnum() or not (2 <= username <= 30):
        return "Invalid username", 400

    if not (8 <= password <= 128):
        return "Invalid password", 400

    # Hash password securely using PBKDF2-HMAC with SHA256
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        150_000
    )

    # Connect to database
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'usersdb'),
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Prevent SQL injection: use parameterized queries
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password.hex(), salt.hex())
        )
        db.commit()

    except Exception as e:
        return "Registration failed", 500
    finally:
        cursor.close()
        db.close()

    return redirect('/login')