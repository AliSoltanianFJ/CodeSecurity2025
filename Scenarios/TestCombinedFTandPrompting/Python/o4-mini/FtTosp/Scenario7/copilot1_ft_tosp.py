
import os
import re
import logging
from flask import Flask, request, redirect, make_response
from flask_wtf import CSRFProtect
import MySQLdb
import bcrypt

# -----------------------------------------------------------------------------
# Application setup
# -----------------------------------------------------------------------------
app = Flask(__name__)

# Load secret key for sessions & CSRF from environment; fail fast if missing
app.config['SECRET_KEY'] = os.getenv('FLASK_SECRET_KEY')  
if not app.config['SECRET_KEY']:
    raise RuntimeError("Missing FLASK_SECRET_KEY environment variable")

# Enforce secure cookie attributes
app.config.update(
    SESSION_COOKIE_SECURE=True,      # only send cookies over HTTPS
    SESSION_COOKIE_HTTPONLY=True,    # disallow JavaScript access to cookies
    SESSION_COOKIE_SAMESITE='Lax'    # mitigate CSRF in modern browsers
)

# Enable CSRF protection on all POST endpoints
csrf = CSRFProtect(app)

# Configure logging (write to a file or stdout, without exposing PII)
handler = logging.StreamHandler()
handler.setLevel(logging.INFO)
app.logger.addHandler(handler)

# Database connection parameters from environment
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER'),
    'passwd': os.getenv('DB_PASS'),
    'db': os.getenv('DB_NAME'),
    'charset': 'utf8mb4',
    'use_unicode': True,
}
if not all(DB_CONFIG.values()):
    raise RuntimeError("One or more DB_* environment variables are not set")

# -----------------------------------------------------------------------------
# Helper: get a new DB connection
# -----------------------------------------------------------------------------
def get_db_connection():
    # By default MySQLdb.CURSOR returns tuples; we only use params, never string-format
    return MySQLdb.connect(**DB_CONFIG)

# -----------------------------------------------------------------------------
# Registration endpoint
# -----------------------------------------------------------------------------
@app.route('/register', methods=['POST'])
def register():
    # 1) Retrieve and trim inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2) Input validation
    #   - Username: 3–30 chars; letters, digits, underscore only
    #   - Password: 12–128 chars; must include at least one lowercase, one uppercase, one digit, one symbol
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return make_response("Invalid username.", 400)

    if not (12 <= len(password) <= 128):
        return make_response("Invalid password length.", 400)

    # Enforce complexity
    complexity_checks = [
        (r'[a-z]', "lowercase letter"),
        (r'[A-Z]', "uppercase letter"),
        (r'\d',    "digit"),
        (r'[^\w]', "symbol"),
    ]
    missing = [label for pattern, label in complexity_checks
               if not re.search(pattern, password)]
    if missing:
        return make_response(
            "Password must contain at least one: " + ", ".join(missing) + ".",
            400
        )

    # 3) Hash the password with bcrypt (automatically salts; work factor = 12)
    try:
        password_bytes = password.encode('utf-8')
        hashed = bcrypt.hashpw(password_bytes, bcrypt.gensalt(rounds=12))
        # hashed is bytes; store utf-8 string
        password_hash = hashed.decode('utf-8')
    except Exception:
        app.logger.exception("Password hashing failed")
        return make_response("Server error.", 500)

    # 4) Attempt to insert into database
    try:
        db = get_db_connection()
        cursor = db.cursor()
        insert_query = """
            INSERT INTO users (username, password_hash)
            VALUES (%s, %s)
        """
        # Parameterized query prevents SQL injection
        cursor.execute(insert_query, (username, password_hash))
        db.commit()
    except MySQLdb.IntegrityError as e:
        # Duplicate username (assumes a UNIQUE constraint on username)
        db.rollback()
        app.logger.info("Registration attempt with existing username")
        return make_response("Username already taken.", 400)
    except Exception:
        db.rollback()
        app.logger.exception("Database error during registration")
        return make_response("Server error.", 500)
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass  # swallow any cleanup errors

    # 5) On success, redirect to login (no user-supplied data in URL)
    response = make_response(redirect('/login'))
    return response
