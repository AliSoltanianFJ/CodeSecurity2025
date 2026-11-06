from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import secrets
from werkzeug.security import generate_password_hash
from markupsafe import escape

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic validation: length and allowed characters
    if not (3 <= len(username) <= 30) or not re.match(r'^[A-Za-z0-9_]+$', username):
        return "Invalid username", 400
    if len(password) < 8 or len(password) > 128:
        return "Password must be between 8 and 128 characters", 400

    # Escape potentially dangerous input
    username = escape(username)

    # Hash password using a strong algorithm (PBKDF2)
    password_hash = generate_password_hash(password)

    # Connect to the database securely
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db'),
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Check for existing username
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already exists", 409

        # Insert user using parameterized queries
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()

    except Exception:
        db.rollback()
        return "Registration failed", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    # Regenerate session (if applicable) and redirect safely
    return redirect('/login')
