from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)


from flask import escape, abort
import secrets

@app.route('/register', methods=['POST'])
def register():
    # Ensure Content-Type is as expected
    if not request.content_type.startswith('application/x-www-form-urlencoded'):
        abort(400, description="Invalid content type.")

    # Validate and sanitise input
    username = request.form.get('username', '', type=str)
    password = request.form.get('password', '', type=str)

    # Enforce length limits
    if not (3 <= len(username) <= 30) or not (8 <= len(password) <= 64):
        abort(400, description="Invalid input length.")

    # Whitelist username (alphanumeric + underscore)
    if not re.fullmatch(r'^\w+$', username):
        abort(400, description="Invalid username.")

    # Sanitize: remove non-printable
    username = re.sub(r'[^\w]', '', username)

    # Prevent XSS: escape output, though not reflected here
    username = escape(username)

    # Prevent buffer overflow: length already checked, use safe types

    # Password: ensure complexity
    if not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) or not re.search(r'\d', password):
        abort(400, description="Password must contain upper, lower, digit.")

    # Never log or expose private info

    # Use strong password hashing (PBKDF2)
    salt = secrets.token_bytes(16)
    hash_bytes = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        200_000
    )
    pwd_hash = salt.hex() + ':' + hash_bytes.hex()

    # Connect to DB securely, use parameterised queries to prevent SQL injection
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME')
        )
        cursor = db.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, pwd_hash)
        )
        db.commit()
    except Exception:
        abort(500, description="Registration failed.")
    finally:
        if 'db' in locals():
            db.close()

    # Never reflect user input in response
    return redirect('/login')
