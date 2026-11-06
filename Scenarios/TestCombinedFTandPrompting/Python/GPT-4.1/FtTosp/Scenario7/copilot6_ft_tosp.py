from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)


@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Input validation: enforce length and character restrictions
    if not (3 <= len(username) <= 30):
        return "Invalid username length", 400
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "Invalid username characters", 400
    if not (8 <= len(password) <= 128):
        return "Invalid password length", 400

    # Sanitize input: remove any potentially dangerous characters
    username = re.sub(r'[<>&\'"]', '', username)

    # Prevent cross-site scripting (XSS) by never reflecting input
    # (No user input is returned in response)

    # Hash the password securely using PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )

    # Do not expose private information

    # Connect to DB using parameterized queries to prevent SQL Injection
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'testdb'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password.hex(), salt.hex())
        )
        db.commit()
    except Exception:
        return "Registration error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    # Never expose sensitive info
    return redirect('/login')
