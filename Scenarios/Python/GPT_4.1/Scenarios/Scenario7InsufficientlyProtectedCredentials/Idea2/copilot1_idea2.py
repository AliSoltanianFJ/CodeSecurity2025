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

    # Input validation: only allow alphanumeric usernames, 3-30 chars
    if not re.match(r'^[a-zA-Z0-9_]{3,30}$', username):
        return "Invalid username.", 400

    # Password validation: at least 8 chars, must contain letters and numbers
    if not re.match(r'^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,}$', password):
        return "Password must be at least 8 characters and contain letters and numbers.", 400

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    hashed_pw = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )
    # Store salt and hash as hex
    salt_hex = salt.hex()
    hash_hex = hashed_pw.hex()

    # Connect to DB securely
    db = MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER', 'root'),
        passwd=os.environ.get('DB_PASS', ''),
        db=os.environ.get('DB_NAME', 'users')
    )
    cursor = db.cursor()

    # Use parameterized query to prevent SQL injection
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hash_hex, salt_hex)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists.", 409
    finally:
        cursor.close()
        db.close()

    # Never expose private info
    return redirect('/login')
# ...existing code...