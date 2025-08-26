from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
#-copilot next line-
    # 1) Validate
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password too weak", 400

    # 2) Hash with a per-user salt
    salt = os.urandom(16)
    pwd_hash = hashlib.pbkdf2_hmac('sha256',
                                   password.encode('utf-8'),
                                   salt,
                                   100_000)
    hash_hex = pwd_hash.hex()
    salt_hex = salt.hex()

    # 3) Store safely
    conn = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME'),
        charset='utf8mb4'
    )
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hash_hex, salt_hex)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        # assumes UNIQUE(username)
        return "Username already taken", 400
    finally:
        cur.close()
        conn.close()

    # 4) Redirect on success
    return redirect('/login')