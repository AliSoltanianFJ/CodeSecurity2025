from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
#-copilot next line-
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 1) Validate inputs
    if not re.match(r'^[A-Za-z0-9_]{3,20}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password must be at least 8 characters", 400

    # 2) Connect to DB (use environment vars for creds!)
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST'),
        user=os.getenv('DB_USER'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME'),
        charset='utf8mb4'
    )
    cursor = db.cursor()

    # 3) Check for existing user
    cursor.execute("SELECT 1 FROM users WHERE username = %s", (username,))
    if cursor.fetchone():
        db.close()
        return "Username already taken", 409

    # 4) Hash & salt password with PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    pwd_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )

    # 5) Store user
    cursor.execute(
        "INSERT INTO users (username, salt, password_hash) VALUES (%s, %s, %s)",
        (username, salt.hex(), pwd_hash.hex())
    )
    db.commit()
    db.close()

    # 6) Redirect to login or return success
    return redirect('/login')