from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)


from flask import escape, abort

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitise input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Input validation: only allow alphanumeric, 3-30 chars
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400, description="Invalid username.")

    # Password validation: min 8, max 64, at least one number and letter
    if not re.fullmatch(r'^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d!@#$%^&*()_+=\-]{8,64}$', password):
        abort(400, description="Invalid password.")

    # Prevent XSS by never reflecting input
    # Never expose input in errors

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    hashed_pw = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )

    # Prepare for secure DB insert
    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'user'),
            passwd=os.environ.get('DB_PASS', 'pass'),
            db=os.environ.get('DB_NAME', 'appdb'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()

        # Use parameterised queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_pw.hex(), salt.hex())
        )
        conn.commit()
    except Exception:
        abort(500, description="Registration failed.")
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'conn' in locals():
            conn.close()

    # Never disclose sensitive info
    return redirect('/login')
