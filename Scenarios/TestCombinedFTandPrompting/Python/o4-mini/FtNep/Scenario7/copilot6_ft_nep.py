import logging
import os
import re
from flask import Flask, request, redirect, abort
from werkzeug.security import generate_password_hash
import MySQLdb
import MySQLdb.cursors

# configure your logger
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

app = Flask(__name__)

# You may also want to configure logging handlers here,
# e.g. StreamHandler or FileHandler, depending on your setup.

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # 1. Collect and normalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Validate username: alphanumeric + underscore, 3–30 chars
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        # Avoid detailed error messages that could aid an attacker
        return "Invalid username or password.", 400

    # 3. Validate password complexity
    #    Require at least 12 chars, one lowercase, one uppercase, one digit, one special
    if (len(password) < 12
        or not re.search(r'[a-z]', password)
        or not re.search(r'[A-Z]', password)
        or not re.search(r'\d', password)
        or not re.search(r'[!@#$%^&*(),.?":{}|<>]', password)
       ):
        return "Invalid username or password.", 400

    # 4. Load DB credentials from environment (fail if missing)
    db_host = os.getenv('DB_HOST')
    db_user = os.getenv('DB_USER')
    db_pass = os.getenv('DB_PASS')
    db_name = os.getenv('DB_NAME')
    if not all((db_host, db_user, db_pass, db_name)):
        logger.error("Database configuration incomplete in environment")
        return "Internal server error.", 500

    # 5. Connect to the database securely
    try:
        db = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            cursorclass=MySQLdb.cursors.DictCursor,
            connect_timeout=5
        )
    except Exception:
        logger.exception("Database connection failed")
        return "Internal server error.", 500

    try:
        cursor = db.cursor()

        # 6. Check for existing username (prevent duplicates)
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s LIMIT 1",
            (username,)
        )
        if cursor.fetchone():
            # 409 Conflict is more semantically correct, but 400 is acceptable
            return "Username already in use.", 409

        # 7. Hash the password with PBKDF2-SHA256 (Werkzeug default):
        #    260,000 iterations, 16-byte salt by default
        password_hash = generate_password_hash(password)

        # 8. Insert new user
        cursor.execute(
            """
            INSERT INTO users (username, password_hash)
            VALUES (%s, %s)
            """,
            (username, password_hash)
        )
        db.commit()

    except Exception:
        db.rollback()
        logger.exception("Error during registration transaction")
        # Generic message to avoid leaking internal state
        return "Internal server error.", 500

    finally:
        cursor.close()
        db.close()

    # 9. On success, redirect to login (use 303 to force GET)
    return redirect('/login', code=303)
