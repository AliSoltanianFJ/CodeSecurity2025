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

    if len(username) > 150 or len(password) > 128:
        return "Input too long", 400

    if not re.match(r'^[A-Za-z0-9_]+$', username):
        return "Invalid username", 400

    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'users')
        )
        cursor = db.cursor()
        cursor.execute("SELECT username FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already exists", 409

        hashed_password = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            os.urandom(16),
            100_000
        ).hex()

        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except Exception as e:
        return "Registration failed", 500
    finally:
        if 'db' in locals():
            db.close()

    return redirect('/login')