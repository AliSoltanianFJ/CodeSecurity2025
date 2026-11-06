from flask import Flask, request, redirect
import re
import hashlib
import os
import logging
import MySQLdb

app = Flask(__name__)

# Configure logging for internal diagnostics
logging.basicConfig(level=logging.INFO)

# Precompile username‐validation regex
USERNAME_RE = re.compile(r'^[A-Za-z0-9_]{3,30}$')
MIN_PASSWORD_LENGTH = 8

@app.route('/register', methods=['POST'])
def register():
    # 1) Fetch and normalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2) Basic presence checks
    if not username or not password:
        return "Username and password are required", 400

    # 3) Validate username pattern
    if not USERNAME_RE.match(username):
        return "Username must be 3–30 characters of letters, digits, or underscore only", 400

    # 4) Enforce a minimum password length
    if len(password) < MIN_PASSWORD_LENGTH:
        return f"Password must be at least {MIN_PASSWORD_LENGTH} characters long", 400

    # 5) Derive a password hash with PBKDF2‐HMAC‐SHA256
    salt = os.urandom(16)
    iterations = 200_000
    # pbkdf2_hmac returns raw bytes; we'll hex‐encode them for storage
    hash_bytes = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        iterations
    )
    salt_hex = salt.hex()         # 32 hex chars
    hash_hex = hash_bytes.hex()   # 64 hex chars

    # 6) Connect to MySQL using environment variables (with sensible defaults)
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db'),
            charset='utf8mb4',
            use_unicode=True
        )
    except MySQLdb.MySQLError:
        logging.exception("Could not connect to the database")
        # Do not reveal internal errors to the client
        return "Internal server error", 500

    try:
        cursor = db.cursor()

        # 7) Check for an existing username
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s",
            (username,)
        )
        if cursor.fetchone():
            return "Username is already taken", 409

        # 8) Insert the new user record
        cursor.execute(
            """
            INSERT INTO users
              (username, password_hash, salt, iterations)
            VALUES
              (%s, %s, %s, %s)
            """,
            (username, hash_hex, salt_hex, iterations)
        )
        db.commit()

    except MySQLdb.IntegrityError:
        # This can catch a UNIQUE violation if there is a race
        db.rollback()
        return "Username is already taken", 409

    except MySQLdb.MySQLError:
        # Any other database error
        db.rollback()
        logging.exception("Database error during registration")
        return "Internal server error", 500

    except Exception:
        # Catch‐all for unexpected errors
        db.rollback()
        logging.exception("Unexpected error during registration")
        return "Internal server error", 500

    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass

    # 9) On success, redirect to the (fixed) login page
    return redirect('/login')
