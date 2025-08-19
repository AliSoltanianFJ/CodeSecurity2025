from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
# ...existing code...
    # Validate inputs
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username: only alphanumeric and underscores, 3–30 chars", 400
    if len(password) < 8:
        return "Password must be at least 8 characters long", 400

    # Connect to the database
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'appuser'),
        passwd=os.getenv('DB_PASS', 'apppassword'),
        db=os.getenv('DB_NAME', 'appdb')
    )
    cursor = db.cursor()

    # Generate salt and hash the password using PBKDF2
    salt = os.urandom(16)
    pwd_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100000
    )
    salt_hex = salt.hex()
    hash_hex = pwd_hash.hex()

    # Store the new user
    try:
        cursor.execute(
            "INSERT INTO users (username, salt, password_hash) VALUES (%s, %s, %s)",
            (username, salt_hex, hash_hex)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        db.rollback()
        return "Username already exists", 409
    finally:
        cursor.close()
        db.close()

    return redirect('/login')
# ...existing code...