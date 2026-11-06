from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Validate input: check existence, length, and allowed characters
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()
    
    # Check for missing fields
    if not username or not password:
        return "Invalid input: Missing username or password", 400

    # Enforce length limits
    if len(username) > 150 or len(password) > 128:
        return "Invalid input: Input too long", 400

    # Allow only alphanumeric and limited safe characters for username
    if not re.match(r'^[a-zA-Z0-9_.-]+$', username):
        return "Invalid input: Invalid characters in username", 400

    # Sanitize input (although input is validated, always sanitize)
    username = re.sub(r'[<>&"\']', '', username)

    # Never log or expose private information like password

    # Use strong password hashing: PBKDF2 with SHA-256 (not plain hashing)
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000  # Number of iterations
    )
    # Store salt and password hash in hex for safety
    salt_hex = salt.hex()
    hash_hex = hashed_password.hex()

    # Prevent SQL Injection: use parameterized queries
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        insert_query = """INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)"""
        cursor.execute(insert_query, (username, hash_hex, salt_hex))
        db.commit()
    except Exception:
        # Don't leak details
        return "Registration failed", 500
    finally:
        cursor.close()
        db.close()

    # Do not expose any private or sensitive information
    # No cross-site scripting since no user input is rendered
    return redirect('/login')
