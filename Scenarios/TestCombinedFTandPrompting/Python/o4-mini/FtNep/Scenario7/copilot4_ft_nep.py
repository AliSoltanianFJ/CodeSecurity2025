from flask import Flask, request, redirect
import re
import os
import MySQLdb
import logging
from werkzeug.security import generate_password_hash

app = Flask(__name__)
app.logger.setLevel(logging.INFO)

# register URL
@app.route('/register', methods=['POST'])
def register():
    # 1. Extract and normalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Basic presence check
    if not username or not password:
        return "Invalid input.", 400

    # 3. Username policy: 3–30 chars, letters, digits, underscore only
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Username must be 3–30 characters; letters, digits or underscores only.", 400

    # 4. Password policy: min 12 chars, at least one uppercase, one lowercase, one digit & one symbol
    if len(password) < 12:
        return "Password must be at least 12 characters long.", 400
    if not re.search(r'[A-Z]', password):
        return "Password must include at least one uppercase letter.", 400
    if not re.search(r'[a-z]', password):
        return "Password must include at least one lowercase letter.", 400
    if not re.search(r'\d', password):
        return "Password must include at least one digit.", 400
    if not re.search(r'\W', password):
        return "Password must include at least one symbol (non-alphanumeric).", 400

    # (Optional) You could check against a small blacklist of extremely common passwords:
    # common = {'password','123456','qwerty', ...}
    # if password.lower() in common:
    #     return "That password is too common.", 400

    # 5. Hash the password with PBKDF2-SHA256 + per-hash salt (16 bytes)
    try:
        # werkzeug.security.generate_password_hash outputs something like:
        #   pbkdf2:sha256:260000$<salt>$<digest>
        password_hash = generate_password_hash(
            password,
            method='pbkdf2:sha256',
            salt_length=16
        )
    except Exception as err:
        app.logger.error("Password hashing failed: %s", err)
        return "Internal server error.", 500

    # 6. Load DB configuration from environment
    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            connect_timeout=5
        )
    except KeyError as err:
        app.logger.error("Database env var missing: %s", err)
        return "Internal server error.", 500
    except MySQLdb.Error as err:
        app.logger.error("Database connection error: %s", err)
        return "Internal server error.", 500

    try:
        cursor = db.cursor()

        # 7. Prevent duplicate usernames
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s LIMIT 1",
            (username,)
        )
        if cursor.fetchone():
            return "Username already taken.", 400

        # 8. Insert new user
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()

    except MySQLdb.Error as err:
        db.rollback()
        app.logger.error("Database query failed: %s", err)
        return "Registration failed.", 500

    finally:
        # 9. Clean up resources
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass

    # 10. On success, redirect to login
    return redirect('/login')
