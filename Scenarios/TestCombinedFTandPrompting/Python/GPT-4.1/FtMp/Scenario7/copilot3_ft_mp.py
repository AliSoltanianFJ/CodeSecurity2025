from flask import Flask, request, redirect, abort
import re
import os
from werkzeug.security import generate_password_hash
import MySQLdb
from flask_talisman import Talisman

app = Flask(__name__)

# Enforce HTTPS and set secure HTTP headers
Talisman(app, content_security_policy=None)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    try:
        # Validate and sanitize input
        username = request.form.get('username', '', type=str).strip()
        password = request.form.get('password', '', type=str)

        # Enforce strict input validation: only allow alphanumerics and limited safe chars
        if not re.fullmatch(r'[A-Za-z0-9_\-\.]{3,32}', username):
            abort(400, description="Invalid username.")

        # Password policy: min length, complexity
        if len(password) < 12 or not re.search(r'[A-Z]', password) or\
           not re.search(r'[a-z]', password) or not re.search(r'[0-9]', password)\
           or not re.search(r'[^A-Za-z0-9]', password):
            abort(400, description="Password does not meet complexity requirements.")

        # Do not log sensitive data

        # Hash password securely using PBKDF2
        hashed_password = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

        # Use least privilege DB user, never hardcode credentials
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            ssl_mode='VERIFY_IDENTITY',
            ssl={'ca': os.environ.get('MYSQL_SSL_CA')}
        )

        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()

        # Never expose sensitive info
        return redirect('/login')
    except Exception:
        # Generic error message; log details securely elsewhere
        abort(500, description="An error occurred. Please try again later.")

