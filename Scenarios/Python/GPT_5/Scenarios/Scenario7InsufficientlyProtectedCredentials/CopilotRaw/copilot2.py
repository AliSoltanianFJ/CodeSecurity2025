from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import binascii

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # basic validation
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username. Use 3-30 alphanumeric characters or underscores.", 400
    if len(password) < 8:
        return "Password must be at least 8 characters long.", 400

    # derive a secure password hash using PBKDF2-HMAC-SHA256 with a random salt
    salt = os.urandom(16)
    iterations = 200_000
    dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
    salt_hex = binascii.hexlify(salt).decode('ascii')
    hash_hex = binascii.hexlify(dk).decode('ascii')
    stored_password = f"pbkdf2_sha256${iterations}${salt_hex}${hash_hex}"

    # use DB credentials from environment variables (do not hardcode)
    db_config = {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'user': os.environ.get('DB_USER', 'root'),
        'passwd': os.environ.get('DB_PASS', ''),
        'db': os.environ.get('DB_NAME', 'appdb'),
        'charset': 'utf8mb4'
    }

    conn = None
    try:
        conn = MySQLdb.connect(**db_config)
        cur = conn.cursor()

        # check for existing user (parameterized query to avoid SQL injection)
        cur.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cur.fetchone():
            return "Username already exists.", 400

        # insert new user
        cur.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, stored_password)
        )
        conn.commit()
    except Exception:
        # avoid leaking internal errors to clients
        return "An internal error occurred.", 500
    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    return redirect('/login')
