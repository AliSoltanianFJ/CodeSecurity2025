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

    # 1) Validate username: 3–20 alphanumeric chars
    if not re.fullmatch(r'[A-Za-z0-9]{3,20}', username):
        return "Invalid username", 400

    # 2) Validate password strength
    if (len(password) < 8
        or not re.search(r'[a-z]', password)
        or not re.search(r'[A-Z]', password)
        or not re.search(r'\d',    password)
        or not re.search(r'\W',    password)):
        return "Password does not meet complexity requirements", 400

    # 3) Connect to the database
    try:
        db = MySQLdb.connect(
            host   = os.environ['DB_HOST'],
            user   = os.environ['DB_USER'],
            passwd = os.environ['DB_PASS'],
            db     = os.environ['DB_NAME'],
            charset='utf8mb4'
        )
    except MySQLdb.Error:
        return "Database connection error", 500

    cursor = db.cursor()

    # 4) Check for existing user
    cursor.execute("SELECT 1 FROM users WHERE username=%s", (username,))
    if cursor.fetchone():
        cursor.close()
        db.close()
        return "Username already taken", 400

    # 5) Generate a salt & derive password hash
    salt = os.urandom(16)  # 128-bit salt
    # 100,000 iterations of SHA-256
    pwd_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )

    # 6) Store (hex-encoded) salt & hash
    cursor.execute(
        "INSERT INTO users (username, salt, passhash) VALUES (%s, %s, %s)",
        (username, salt.hex(), pwd_hash.hex())
    )
    db.commit()
    cursor.close()
    db.close()

    # 7) On success, redirect to login
    return redirect('/login')