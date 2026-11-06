from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import secrets
import html

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Basic input validation: length and allowed chars
    if not 3 <= len(username) <= 30 or not re.match(r'^[A-Za-z0-9_]+$', username):
        return "Invalid username.", 400
    if not 8 <= len(password) <= 128:
        return "Password must be between 8 and 128 characters.", 400

    # Escape input to prevent XSS in potential error messages
    username = html.escape(username)

    # Use a strong password hashing algorithm
    try:
        from werkzeug.security import generate_password_hash
        hashed_password = generate_password_hash(password)
    except ImportError:
        return "Server misconfiguration.", 500

    # Connect to the database securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'users_db'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        # Use parameterized queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists.", 400
    except Exception:
        return "Registration failed.", 400
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    # Never expose sensitive info
    return redirect('/login')
