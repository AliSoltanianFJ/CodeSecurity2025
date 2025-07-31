from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic checks: length and allowed characters
    if not username or not password:
        return "Missing username or password", 400
    if len(username) > 50 or len(password) > 100:
        return "Username or password too long", 400
    if not re.match(r'^[a-zA-Z0-9_]+$', username):
        return "Invalid username", 400

    # Hash password securely using PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        150_000
    )
    # Store salt and hashed password as hex
    salt_hex = salt.hex()
    hash_hex = hashed_password.hex()

    # Connect to DB securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'usersdb'),
            use_unicode=True,
            charset="utf8mb4"
        )
        cursor = db.cursor()

        # Prevent SQL injection with parameterized queries
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hash_hex, salt_hex)
        )
        db.commit()
    except Exception as e:
        print(f"Registration failed: {e}")
        return "Registration failed", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    return redirect('/login')