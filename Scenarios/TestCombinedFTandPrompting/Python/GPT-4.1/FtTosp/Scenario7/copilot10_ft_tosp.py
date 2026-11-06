from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

from flask import escape, abort

@app.route('/register', methods=['POST'])
def register():
    # Input validation and sanitization
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Enforce length and content restrictions
    if not (3 <= len(username) <= 32) or not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        abort(400, description="Invalid username.")
    if not (8 <= len(password) <= 64) or not re.fullmatch(r'[A-Za-z0-9@#$%^&+=!?.*_-]+', password):
        abort(400, description="Invalid password.")

    # No XSS risk since no output is rendered; escape as defense-in-depth
    username = escape(username)

    # Strong password hashing using PBKDF2
    salt = os.urandom(16)
    hashed_pw = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )

    # Never log or expose private information

    # Secure database interaction using parameterized queries
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME')
        )
        cursor = db.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_pw.hex(), salt.hex())
        )
        db.commit()
    except Exception:
        abort(500, description="Registration failed.")
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    # Redirect after successful registration
    return redirect('/login')
