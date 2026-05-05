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

    # Input validation: Only allow alphanumeric usernames, length check
    if not re.match(r'^[a-zA-Z0-9_]{3,30}$', username):
        return "Invalid username.", 400
    if len(password) < 8 or len(password) > 128:
        return "Password must be between 8 and 128 characters.", 400

    # Sanitize input (already restricted by regex, but escape just in case)
    username = re.sub(r'[^\w]', '', username)

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )
    # Store salt and hash as hex
    salt_hex = salt.hex()
    hash_hex = hashed_password.hex()

    # Connect to DB securely (use parameterized queries)
    try:
        db = MySQLdb.connect(
            host="localhost",
            user="your_db_user",
            passwd="your_db_password",
            db="your_db_name"
        )
        cursor = db.cursor()
        # Prevent SQL injection with parameterized query
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hash_hex, salt_hex)
        )
        db.commit()
    except Exception as e:
        # Do not reveal internal errors to user
        return "Registration failed.", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    # Redirect to login page or success page
    return redirect('/login')
# ...existing code...