import os
import re
import logging

from flask import Flask, request, redirect, abort, make_response
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from passlib.hash import argon2
import mysql.connector
from mysql.connector import errorcode, IntegrityError

# -----------------------------------------------------------------------------
# Configuration and secure defaults
# -----------------------------------------------------------------------------
# Application secret key (used for session cookies, CSRF tokens, etc.)
# Must be set in the environment; do not fall back to a hard-coded default.
SECRET_KEY = os.getenv('FLASK_SECRET_KEY')
if not SECRET_KEY:
    raise RuntimeError("Environment variable FLASK_SECRET_KEY must be set and non-empty")

app = Flask(__name__)
app.config['SECRET_KEY'] = SECRET_KEY

# Secure cookie settings
app.config.update(
    SESSION_COOKIE_SECURE=True,       # HTTPS only
    SESSION_COOKIE_HTTPONLY=True,     # Not accessible to JavaScript
    SESSION_COOKIE_SAMESITE='Strict'  # CSRF protection
)

# Rate limiter to mitigate brute-force / enumeration attacks
limiter = Limiter(
    app,
    key_func=get_remote_address,
    default_limits=["10 per minute"],    # Adjust to your risk profile
)

# Configure logging (avoid logging sensitive data)
logging.basicConfig(
    filename='app.log',
    level=logging.INFO,
    format='%(asctime)s %(levelname)s %(message)s'
)

# -----------------------------------------------------------------------------
# Database configuration: read from environment variables only
# The DB user should have only the minimum privileges necessary (INSERT on users).
# SSL is enforced to protect data in transit.
# -----------------------------------------------------------------------------
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER'),
    'password': os.getenv('DB_PASSWORD'),
    'database': os.getenv('DB_NAME'),
    'ssl_ca': os.getenv('DB_SSL_CA'),          # Path to CA cert for SSL
    'ssl_verify_cert': True,
    'charset': 'utf8mb4',                     # Support full Unicode safely
    'use_unicode': True,
    'connection_timeout': 5                    # Fail fast on connection issues
}
for key in ('user','password','database','ssl_ca'):
    if not DB_CONFIG.get(key):
        raise RuntimeError(f"Environment variable for DB_{key.upper()} must be set")

# -----------------------------------------------------------------------------
# Input validation regexes
# -----------------------------------------------------------------------------
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,30}$')  
# Minimum 12 chars, at least one lowercase, one uppercase, one digit, one special
PASSWORD_REGEX = re.compile(
    r'^(?=.{12,}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).*$'
)

# -----------------------------------------------------------------------------
# register endpoint
# -----------------------------------------------------------------------------
@app.route('/register', methods=['POST'])
@limiter.limit("5 per minute")  # stricter rate‐limit on registration
def register():
    try:
        # 1. Extract and normalize inputs
        username = request.form.get('username', '').strip()
        password = request.form.get('password', '')

        # 2. Input validation
        if not USERNAME_REGEX.fullmatch(username):
            # Do not reveal which rule failed
            abort(400, description="Invalid input")
        if not PASSWORD_REGEX.fullmatch(password):
            abort(400, description="Invalid input")

        # 3. Hash the password with Argon2 using secure parameters
        #    Argon2 automatically generates a salt and embeds it in the hash output.
        #    We rely on default time_cost/memory_cost/parallelism tuned by passlib.
        password_hash = argon2.using(
            time_cost=3,
            memory_cost=102400,
            parallelism=8,
            salt_size=16,
            hash_len=32
        ).hash(password)

        # 4. Store the new user in the database using a parameterized query.
        #    The DB user account should only have INSERT privilege on the users table.
        conn = mysql.connector.connect(**DB_CONFIG)
        try:
            with conn.cursor(prepared=True) as cursor:
                sql = """
                    INSERT INTO users (username, password_hash, created_at)
                    VALUES (%s, %s, UTC_TIMESTAMP())
                """
                cursor.execute(sql, (username, password_hash))
            conn.commit()
        finally:
            conn.close()

        # 5. On success, redirect to login or a "check your email" page
        response = make_response(redirect('/login'))
        # Security headers
        response.headers['Content-Security-Policy'] = \
            "default-src 'self'; frame-ancestors 'none'; object-src 'none'"
        return response

    except IntegrityError as ie:
        # Duplicate username or constraint violation
        logging.info("Attempt to register existing username")
        # Always return a generic message to avoid username enumeration
        abort(400, description="Registration failed")

    except mysql.connector.Error as db_err:
        # Log details for internal diagnostics only
        logging.error("Database error during registration: %s", db_err, exc_info=True)
        # Do not leak internal details to client
        abort(500, description="Internal server error")

    except Exception as ex:
        # Catch-all for unexpected errors
        logging.error("Unexpected error during registration", exc_info=True)
        abort(500, description="Internal server error")
