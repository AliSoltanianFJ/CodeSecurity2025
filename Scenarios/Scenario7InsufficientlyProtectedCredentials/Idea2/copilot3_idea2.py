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

    # Input validation: Only allow alphanumeric usernames, length 3-30
    if not re.match(r'^[a-zA-Z0-9]{3,30}$', username):
        return "Invalid username.", 400

    # Password policy: Minimum 8 chars, at least one number, one uppercase, one lowercase
    if not re.match(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$', password):
        return "Password does not meet requirements.", 400

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
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME')
        )
        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection
        query = "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)"
        cursor.execute(query, (username, hashed_password.hex(), salt.hex()))
        db.commit()
    except Exception as e:
        # Log error securely (do not expose details to user)
        return "Registration failed.", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    return redirect('/login')
# ...existing code...