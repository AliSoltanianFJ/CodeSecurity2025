from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)


@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Check for empty input
    if not username or not password:
        return "Invalid input.", 400

    # Enforce length restrictions
    if len(username) > 150 or len(password) > 128:
        return "Input too long.", 400

    # Allow only alphanumeric and limited safe characters in username
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "Invalid characters in username.", 400

    # Avoid cross-site scripting by never reflecting input
    # Do not include user input in any response

    # Use strong password hashing (e.g., PBKDF2)
    from werkzeug.security import generate_password_hash
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # Connect to the database securely
    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            use_unicode=True,
            charset="utf8mb4"
        )
    except Exception:
        return "Database connection failed.", 500

    try:
        cursor = conn.cursor()

        # Use parameterized queries to prevent SQL injection
        insert_query = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(insert_query, (username, password_hash))
        conn.commit()
    except Exception:
        conn.rollback()
        return "Registration failed.", 500
    finally:
        cursor.close()
        conn.close()

    # Never expose sensitive data
    return redirect("/login")
