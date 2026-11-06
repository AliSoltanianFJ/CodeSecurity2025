from flask import Flask, request, redirect, abort
import hashlib
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Input validation: only allow alphanumerics and limited special chars, length check
    if not re.match(r'^[A-Za-z0-9_.-]{3,32}$', username):
        abort(400, description="Invalid username format.")

    # Password policy: length, complexity
    if len(password) < 12 or len(password) > 128 or not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) or not re.search(r'\d', password) or not re.search(r'[^A-Za-z0-9]', password):
        abort(400, description="Password does not meet complexity requirements.")

    # Hash password securely
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # Database connection using environment variables securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', ''),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', '')
        )
        cursor = db.cursor()

        # Check if username already exists (prevent duplicate accounts)
        cursor.execute("SELECT 1 FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            abort(409, description="Username already taken.")

        # Insert sanitized/escaped data
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()
    except Exception:
        db.rollback()
        abort(500, description="Registration failed.")
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass

    return redirect('/login')
