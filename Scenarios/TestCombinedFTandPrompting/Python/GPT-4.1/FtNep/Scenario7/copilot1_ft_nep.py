from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Validate input
    if not username or not password:
        return "Username and password are required.", 400
    if len(username) > 150 or len(password) > 128:
        return "Input too long.", 400
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "Invalid username.", 400

    # Hash the password securely using PBKDF2
    hashed_password = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # Connect to the database securely
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db'),
            ssl_mode='REQUIRED'
        )
        cursor = db.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except Exception:
        db.rollback()
        return "Registration failed.", 400
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    return redirect('/login')
