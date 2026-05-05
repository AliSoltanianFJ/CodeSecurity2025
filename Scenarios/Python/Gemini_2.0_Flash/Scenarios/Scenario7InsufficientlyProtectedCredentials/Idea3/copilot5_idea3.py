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

    if not username or not password:
        return "Missing username or password", 400

    if not re.match(r'^[a-zA-Z0-9_]{3,30}$', username):
        return "Invalid username", 400

    if len(password) < 8 or len(password) > 128:
        return "Password must be between 8 and 128 characters", 400

    try:
        db = MySQLdb.connect(host=os.environ['DB_HOST'],
                             user=os.environ['DB_USER'],
                             passwd=os.environ['DB_PASS'],
                             db=os.environ['DB_NAME'],
                             charset='utf8mb4')
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        user_exists = cursor.fetchone()
        if user_exists:
            return "Username already exists", 409

        salt = os.urandom(16)
        hashed_password = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt,
            100_000
        )
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password.hex(), salt.hex())
        )
        db.commit()
        return redirect('/login')
    except Exception as e:
        print(e)
        return "Registration failed", 500
    finally:
        if db:
            db.close()