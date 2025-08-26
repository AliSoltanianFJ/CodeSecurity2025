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

    # 1) Basic input validation
    if not re.match(r'^[A-Za-z0-9_]{3,20}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password too short", 400

    # 2) Password hashing with per-user salt
    salt = os.urandom(16)  # 128‐bit salt
    dk = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000          # iterations
    )
    # Store as hex: salt + hash, joined by $
    password_hash = salt.hex() + '$' + dk.hex()

    # 3) Safe insertion into MySQL
    try:
        conn = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4'
        )
        cur = conn.cursor()
        sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cur.execute(sql, (username, password_hash))
        conn.commit()
    except MySQLdb.IntegrityError:
        # e.g. UNIQUE(username) violation
        return "Username already taken", 409
    finally:
        cur.close()
        conn.close()

    # 4) Redirect to login page (or wherever)
    return redirect('/login')