from flask import Flask, request, redirect, render_template_string
import hashlib
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash
from markupsafe import escape

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Input validation
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic length and character checks
    if not (3 <= len(username) <= 30):
        return "Invalid username length", 400
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "Invalid username characters", 400

    # Password complexity check
    if len(password) < 8 or not re.search(r'[A-Z]', password) or \
       not re.search(r'[a-z]', password) or not re.search(r'[0-9]', password):
        return "Password must be at least 8 characters and contain upper, lower, and digit", 400

    # Sanitize username for display (avoid XSS)
    safe_username = escape(username)

    # Securely hash the password using Werkzeug (PBKDF2)
    hashed_password = generate_password_hash(password)

    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db'),
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Use parameterized query to avoid SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists", 409
    except Exception:
        return "Registration failed", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    # Never expose sensitive data
    return render_template_string(
        "<p>Registration successful for user: {{ username }}</p>",
        username=safe_username
    )
