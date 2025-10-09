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

    # Input validation: Only allow alphanumeric usernames, length 3-30
    if not re.match(r'^[a-zA-Z0-9]{3,30}$', username):
        return "Invalid username.", 400

    # Password validation: Minimum length, complexity
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

    # Connect to DB securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME']
        )
        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection
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