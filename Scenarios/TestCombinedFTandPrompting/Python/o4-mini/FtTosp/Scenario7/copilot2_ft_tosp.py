import os
import re
import logging

from flask import Flask, request, redirect, abort
from flask import current_app as app
from passlib.hash import bcrypt
import bleach
import MySQLdb

# Initialize Flask
app = Flask(__name__)

# Configure logging (writes to stderr or a file as you prefer)
logging.basicConfig(level=logging.INFO)

# Compile once at module load
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,30}$')
PASSWORD_MIN_LEN = 8
PASSWORD_MAX_LEN = 128

def get_db_connection():
    """
    Establish a secure MySQL connection using environment variables.
    """
    try:
        return MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            port=int(os.getenv('DB_PORT', '3306')),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True,
            ssl={
                # Point to your CA bundle if you require SSL verification
                'ca': os.getenv('MYSQL_SSL_CA'),
            } if os.getenv('MYSQL_SSL_CA') else None,
            connect_timeout=5
        )
    except Exception as e:
        app.logger.error("Database connection failed", exc_info=e)
        # Fail closed on DB config errors
        abort(500)

def sanitize_username(raw_username):
    """
    Trim whitespace and sanitize against HTML injection.
    We then enforce a strict regex pattern.
    """
    username = raw_username.strip()
    # Reject if HTML tags are detected after cleaning
    username = bleach.clean(username, tags=[], strip=True)
    if not USERNAME_REGEX.fullmatch(username):
        return None
    return username

def validate_password(password):
    """
    Enforce length and complexity rules.
    """
    if not (PASSWORD_MIN_LEN <= len(password) <= PASSWORD_MAX_LEN):
        return False
    # Require at least one uppercase, one lowercase, one digit
    if not re.search(r'[A-Z]', password):
        return False
    if not re.search(r'[a-z]', password):
        return False
    if not re.search(r'\d', password):
        return False
    # Optionally check for special characters, dictionary words, etc.
    return True

@app.route('/register', methods=['POST'])
def register():
    # 1) Retrieve raw inputs
    raw_username = request.form.get('username', '')
    raw_password = request.form.get('password', '')

    # 2) Sanitize & validate inputs
    username = sanitize_username(raw_username)
    if not username:
        # Do not reveal which field failed
        abort(400)

    if not validate_password(raw_password):
        abort(400)

    # 3) Hash the password securely
    try:
        # bcrypt rounds default is 12; adjust work factor via env if needed
        pw_hash = bcrypt.using(rounds=int(os.getenv('BCRYPT_ROUNDS', '12'))).hash(raw_password)
    except Exception as e:
        app.logger.error("Password hashing failed", exc_info=e)
        abort(500)

    # 4) Insert into database using parameterized queries
    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        insert_sql = """
            INSERT INTO users (username, password_hash, created_at)
            VALUES (%s, %s, NOW())
        """
        cursor.execute(insert_sql, (username, pw_hash))
        conn.commit()
    except MySQLdb.IntegrityError:
        # Most likely duplicate username
        # Log internally, return a generic message
        app.logger.info("Attempt to register existing username: %s", username)
        # Redirect back with a generic failure indicator
        return redirect('/register?error=1', code=303)
    except Exception as e:
        app.logger.error("User registration failed", exc_info=e)
        abort(500)
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

    # 5) On success, redirect to login with a neutral flag
    return redirect('/login?registered=1', code=303)

# 6) Generic error handlers to avoid leaking internal details
@app.errorhandler(400)
def bad_request(e):
    return "Bad Request", 400

@app.errorhandler(500)
def internal_error(e):
    return "An unexpected error occurred. Please try again later.", 500
