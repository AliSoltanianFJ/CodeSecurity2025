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
    # Validate username: 3–30 chars, alphanumeric or underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username", 400
    # Validate password strength
    if len(password) < 8:
        return "Password must be at least 8 characters long", 400
    # Hash the password securely
    from werkzeug.security import generate_password_hash
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    # Database operations with parameterized queries
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4'
        )
        with conn.cursor() as cursor:
            cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
            if cursor.fetchone():
                return "Username already exists", 409
            cursor.execute(
                "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                (username, password_hash)
            )
            conn.commit()
    except MySQLdb.Error:
        app.logger.exception("Database error during registration")
        return "Internal server error", 500
    finally:
        try:
            conn.close()
        except NameError:
            pass
    return redirect('/login'), 302
# ...existing code...