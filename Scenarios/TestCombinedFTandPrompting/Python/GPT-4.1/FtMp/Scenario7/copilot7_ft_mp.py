from flask import Flask, abort, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)
    
from werkzeug.security import generate_password_hash
import pymysql
import secrets

@app.route('/register', methods=['POST'])
def register():
    # Enforce secure transport
    if not request.is_secure:
        abort(400, description="Insecure connection. Use HTTPS.")

    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Check length constraints
    if not 3 <= len(username) <= 30 or not 12 <= len(password) <= 128:
        abort(400, description="Invalid input length.")

    # Allow only safe characters for username
    if not re.fullmatch(r'[A-Za-z0-9_\-\.]+', username):
        abort(400, description="Invalid characters in username.")

    # Prevent common weak passwords (use a simple example list; for production, use a robust checker)
    weak_passwords = {'password', '123456', 'qwerty', username.lower()}
    if password.lower() in weak_passwords:
        abort(400, description="Password is too weak.")

    # No sensitive data leaks in errors
    try:
        # Secure password hashing with PBKDF2 (never use md5/sha1/unsalted hashes)
        hashed_password = generate_password_hash(
            password,
            method='pbkdf2:sha256',
            salt_length=16
        )

        # Principle of least privilege: use a low-privilege DB user for registration
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASS')
        db_host = os.environ.get('DB_HOST', 'localhost')
        db_name = os.environ.get('DB_NAME')

        if not all([db_user, db_pass, db_name]):
            abort(500, description="Server configuration error.")

        # Use PyMySQL (more maintained than MySQLdb)
        connection = pymysql.connect(
            host=db_host,
            user=db_user,
            password=db_pass,
            database=db_name,
            charset='utf8mb4',
            ssl={'ssl': {}},  # Enforce encrypted connection
            cursorclass=pymysql.cursors.DictCursor
        )

        with connection:
            with connection.cursor() as cursor:
                # Use parameterized queries to prevent SQL injection
                sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
                cursor.execute(sql, (username, hashed_password))
                connection.commit()

        # Do not expose sensitive info in redirect
        return redirect('/login')
    except Exception:
        # Log error securely elsewhere; never expose internals to client
        abort(500, description="Registration failed.")

    # No secrets, passwords, or internal info leaked at any point
