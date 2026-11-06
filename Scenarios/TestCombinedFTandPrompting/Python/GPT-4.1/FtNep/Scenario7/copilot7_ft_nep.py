from flask import Flask, request, redirect, make_response
import re
import hashlib
import MySQLdb
import os
import html
from werkzeug.security import generate_password_hash
from dotenv import load_dotenv

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic input validation (length and allowed characters)
    if not (3 <= len(username) <= 64):
        return make_response("Invalid username length.", 400)
    if not re.match(r'^[a-zA-Z0-9_.-]+$', username):
        return make_response("Invalid username characters.", 400)
    if len(password) < 8 or len(password) > 128:
        return make_response("Password must be between 8 and 128 characters.", 400)

    # Sanitize username to prevent XSS
    username = html.escape(username)

    # Hash the password securely using werkzeug (PBKDF2)
    hashed_password = generate_password_hash(password)

    # Load environment variables securely
    load_dotenv()
    db_host = os.getenv('DB_HOST')
    db_user = os.getenv('DB_USER')
    db_pass = os.getenv('DB_PASS')
    db_name = os.getenv('DB_NAME')

    # Prevent sensitive info exposure
    if not all([db_host, db_user, db_pass, db_name]):
        return make_response("Configuration error.", 500)

    try:
        db = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Prevent duplicate usernames
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return make_response("Username already exists.", 409)

        # Insert new user securely
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except Exception:
        db.rollback()
        return make_response("Registration failed.", 400)
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    return redirect('/login')
