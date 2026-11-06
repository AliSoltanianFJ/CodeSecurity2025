from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os
import bcrypt

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic input validation
    if not username or not password:
        abort(400, description="Username and password are required.")
    
    # Only allow safe characters (alphanumeric and limited safe symbols)
    if not re.match(r'^[A-Za-z0-9_.-]{3,32}$', username):
        abort(400, description="Invalid username format.")

    # Enforce strong password policy
    if len(password) < 12 or not re.search(r'[A-Z]', password) \
       or not re.search(r'[a-z]', password) \
       or not re.search(r'[0-9]', password) \
       or not re.search(r'[^A-Za-z0-9]', password):
        abort(400, description="Password must be at least 12 characters and include uppercase, lowercase, digit, and special character.")

    # Hash password securely with bcrypt
    try:
        password_bytes = password.encode('utf-8')
        salt = bcrypt.gensalt()
        hashed_password = bcrypt.hashpw(password_bytes, salt)
    except Exception:
        abort(500, description="Password hashing failed.")

    # Connect to the database using environment variables securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'users_db'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
    except Exception:
        abort(500, description="Database connection failed.")

    # Store user securely, prevent duplicate usernames
    try:
        cursor.execute(
            "SELECT id FROM users WHERE username = %s", (username,)
        )
        if cursor.fetchone():
            abort(400, description="Username already exists.")

        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password.decode('utf-8'))
        )
        db.commit()
    except Exception:
        db.rollback()
        abort(500, description="Registration failed.")
    finally:
        cursor.close()
        db.close()

    # Never expose sensitive data
    return redirect('/login')
