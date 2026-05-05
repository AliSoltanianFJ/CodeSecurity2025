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
    password = request.form.get('password', '').strip()

    # Input validation: only allow alphanumeric usernames, length 3-40
    if not re.fullmatch(r'[A-Za-z0-9]{3,40}', username):
        return "Invalid username.", 400

    # Validate password: minimum length, complexity
    if len(password) < 8 or not re.search(r'[A-Z]', password) \
       or not re.search(r'[a-z]', password) \
       or not re.search(r'[0-9]', password):
        return "Password is too weak.", 400

    # Hash password securely using PBKDF2
    salt = os.urandom(16)
    hashed_pw = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100000
    )

    # Connect to DB
    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME')
        )
        cursor = conn.cursor()
        # This prevents SQL injection by using parameterized queries
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_pw.hex(), salt.hex())
        )
        conn.commit()
    except Exception as e:
        return "Registration failed.", 500
    finally:
        try:
            cursor.close()
            conn.close()
        except:
            pass

    # Do not expose sensitive information
    return redirect('/login')