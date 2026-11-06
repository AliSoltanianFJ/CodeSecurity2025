import os
import re
import hashlib
import MySQLdb
from flask import Flask, request, redirect, current_app

app = Flask(__name__)

# You should configure your Flask logger appropriately elsewhere in your app,
# e.g. via app.logger.setLevel(), file handlers, etc.

@app.route('/register', methods=['POST'])
def register():
    # 1. Retrieve & sanitize inputs
    raw_username = request.form.get('username', '')
    raw_password = request.form.get('password', '')

    username = raw_username.strip()
    password = raw_password  # do not strip password, spaces may be intentional

    # 2. Presence check
    if not username or not password:
        return "Username and password are required", 400

    # 3. Username validation: only letters, digits, underscore; 3–30 chars
    if not re.fullmatch(r'\w{3,30}', username):
        return "Invalid username", 400

    # 4. Password policy: at least 12 chars, must include upper, lower, digit, symbol
    if len(password) < 12:
        return "Password must be at least 12 characters long", 400
    if (not re.search(r'[A-Z]', password) or
        not re.search(r'[a-z]', password) or
        not re.search(r'\d', password) or
        not re.search(r'\W', password)):
        return "Password must include uppercase, lowercase, digit, and symbol", 400

    # 5. Hash the password with PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    iterations = 200_000
    # derive a 32-byte key
    dk = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        iterations
    )
    salt_hex = salt.hex()
    dk_hex = dk.hex()
    # Store in format: algorithm$iterations$salt$hash
    password_hash = f"pbkdf2_sha256${iterations}${salt_hex}${dk_hex}"

    # 6. Load DB config from environment (no hardcoding)
    db_params = {
        'host':     os.getenv('DB_HOST'),
        'user':     os.getenv('DB_USER'),
        'passwd':   os.getenv('DB_PASS'),
        'db':       os.getenv('DB_NAME'),
        'charset':  'utf8mb4',
        'connect_timeout': 5
    }
    # Ensure none are missing
    if any(v is None for v in db_params.values()):
        current_app.logger.error("Database configuration is incomplete or missing")
        return "Server configuration error", 500

    # 7. Connect to the database
    try:
        conn = MySQLdb.connect(**db_params)
    except MySQLdb.Error as e:
        current_app.logger.error("Database connection failed: %s", e)
        return "Server error", 500

    try:
        cursor = conn.cursor()

        # 8. Check for existing username
        try:
            cursor.execute(
                "SELECT 1 FROM users WHERE username = %s LIMIT 1",
                (username,)
            )
            if cursor.fetchone():
                # 409 Conflict when username is already taken
                return "Username already exists", 409
        except MySQLdb.Error as e:
            current_app.logger.error("Error checking username existence: %s", e)
            return "Server error", 500

        # 9. Insert the new user
        try:
            cursor.execute(
                "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                (username, password_hash)
            )
            conn.commit()
        except MySQLdb.Error as e:
            conn.rollback()
            current_app.logger.error("Error inserting new user: %s", e)
            return "Registration failed", 500

    finally:
        # 10. Clean up
        cursor.close()
        conn.close()

    # 11. On success, redirect to login (HTTP 302)
    return redirect('/login')
