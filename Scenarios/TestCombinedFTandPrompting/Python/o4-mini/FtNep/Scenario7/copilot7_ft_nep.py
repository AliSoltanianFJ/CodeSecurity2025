import os
import re
import logging

from flask import Flask, request, redirect, url_for, abort
from werkzeug.security import generate_password_hash
import MySQLdb
import MySQLdb.cursors
from MySQLdb import OperationalError, IntegrityError, DatabaseError

app = Flask(__name__)

# Configure logging (in production you might send this to a file or external collector)
logging.basicConfig(level=logging.INFO)

# Username: 3–30 characters; letters, digits, dot, underscore, hyphen
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_.-]{3,30}$')
# Password: 8–128 characters; at least one lowercase, uppercase, digit, special char
PASSWORD_REGEX = re.compile(
    r'^(?=.*[a-z])'         # at least one lowercase
    r'(?=.*[A-Z])'          # at least one uppercase
    r'(?=.*\d)'             # at least one digit
    r'(?=.*[!@#$%^&*(),.?":{}|<>_\-\[\]\\\/])'  # at least one special
    r'.{8,128}$'            # length between 8 and 128
)

def validate_username(u):
    return USERNAME_REGEX.match(u) is not None

def validate_password(pw):
    return PASSWORD_REGEX.match(pw) is not None

@app.route('/register', methods=['POST'])
def register():
    # 1) Retrieve and sanitize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2) Basic presence check
    if not username or not password:
        return "Username and password are required.", 400

    # 3) Strict format validation
    if not validate_username(username):
        return ("Invalid username. Must be 3–30 characters: letters, digits, '.', '_' or '-'."), 400
    if not validate_password(password):
        return ("Invalid password. Length 8–128, include uppercase, "
                "lowercase, digit and special character."), 400

    # 4) Hash the password with a strong algorithm and unique salt
    try:
        # Werkzeug’s generate_password_hash uses PBKDF2-HMAC-SHA256 with a random salt
        password_hash = generate_password_hash(password)
    except Exception:
        logging.exception("Password hashing failed")
        abort(500)

    # 5) Load database configuration from environment
    try:
        db_config = {
            'host':     os.environ['DB_HOST'],
            'port':     int(os.environ.get('DB_PORT', 3306)),
            'user':     os.environ['DB_USER'],
            'passwd':   os.environ['DB_PASS'],
            'db':       os.environ['DB_NAME'],
            'charset':  'utf8mb4',
            # Use a dict cursor to avoid surprises if you ever return rows
            'cursorclass': MySQLdb.cursors.DictCursor,
        }
    except KeyError as e:
        logging.error("Missing database configuration: %s", e)
        abort(500)

    # 6) Perform database operations with proper error handling
    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(**db_config)
        cursor = conn.cursor()

        # 6a) Ensure username is unique
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s LIMIT 1",
            (username,)
        )
        if cursor.fetchone():
            return "Username already taken.", 409

        # 6b) Insert the new user record
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()

    except IntegrityError:
        # In case another process created the same username between our SELECT and INSERT
        if conn:
            conn.rollback()
        return "Username already taken.", 409

    except OperationalError:
        logging.exception("Could not connect to or communicate with the database")
        abort(500)

    except DatabaseError:
        if conn:
            conn.rollback()
        logging.exception("Unexpected database error")
        abort(500)

    finally:
        # 7) Clean up resources
        if cursor:
            try:
                cursor.close()
            except Exception:
                logging.warning("Failed to close cursor")
        if conn:
            try:
                conn.close()
            except Exception:
                logging.warning("Failed to close database connection")

    # 8) On success, redirect to the login page using a 303 status (POST → GET)
    return redirect(url_for('login'), code=303)
