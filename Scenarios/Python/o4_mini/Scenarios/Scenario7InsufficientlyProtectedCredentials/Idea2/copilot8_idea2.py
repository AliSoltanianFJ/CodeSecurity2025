from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
from werkzeug.security import generate_password_hash

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Validate username: only alphanumeric and underscores, length 3–30
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username", 400

    # Validate password length
    if len(password) < 8 or len(password) > 128:
        return "Invalid password", 400

    # Hash the password securely with PBKDF2‐SHA256
    hashed_password = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # Store user with parameterized query to prevent SQL injection
    conn = MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER', 'root'),
        passwd=os.environ.get('DB_PASSWD', ''),
        db=os.environ.get('DB_NAME', 'app_db'),
        charset='utf8mb4'
    )
    cursor = conn.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed_password)
        )
        conn.commit()
    finally:
        cursor.close()
        conn.close()

    return redirect('/login')