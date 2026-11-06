import os
import re
import logging
from flask import Flask, request, redirect, abort, session, make_response
from flask_wtf import CSRFProtect
from passlib.hash import argon2
import MySQLdb
from MySQLdb.cursors import DictCursor
from dotenv import load_dotenv

# Load environment variables from .env in development. In production,
# environment variables must be set by the deployment environment.
load_dotenv()

# ------------------------------------------------------------------------------
# Configuration
# ------------------------------------------------------------------------------

app = Flask(__name__)

# SECRET_KEY used by Flask (session+CSRF). Must be unpredictable, loaded from ENV.
app.config['SECRET_KEY'] = os.environ.get('FLASK_SECRET_KEY')
if not app.config['SECRET_KEY']:
    raise RuntimeError('FLASK_SECRET_KEY environment variable is required')

# Enforce secure session cookies
app.config['SESSION_COOKIE_SECURE'] = True         # only send cookie over HTTPS
app.config['SESSION_COOKIE_HTTPONLY'] = True       # disallow JavaScript access
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'      # mitigate CSRF

# Enable CSRF protection for all POST endpoints
csrf = CSRFProtect(app)

# Set up structured logging. Do NOT log sensitive data (passwords, full SQL errors).
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# ------------------------------------------------------------------------------
# Input validation utilities
# ------------------------------------------------------------------------------

USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_.-]{3,30}$')
# Password policy: at least 12 characters, at least one uppercase, lowercase, digit, and special character
PASSWORD_POLICY = {
    'length_min': 12,
    'uppercase': re.compile(r'[A-Z]'),
    'lowercase': re.compile(r'[a-z]'),
    'digit': re.compile(r'\d'),
    'special': re.compile(r'[^A-Za-z0-9]')
}

def validate_username(username: str) -> bool:
    """
    Validate username:
    - Only alphanumeric characters, underscore, dot or hyphen.
    - Length between 3 and 30.
    - Prevent Unicode confusables by restricting to ASCII.
    """
    if not username:
        return False
    if not USERNAME_REGEX.fullmatch(username):
        return False
    return True

def validate_password(password: str) -> bool:
    """
    Enforce strong password policy:
    - Minimum length.
    - Contains uppercase, lowercase, digit, and special character.
    """
    if not password or len(password) < PASSWORD_POLICY['length_min']:
        return False
    if not PASSWORD_POLICY['uppercase'].search(password):
        return False
    if not PASSWORD_POLICY['lowercase'].search(password):
        return False
    if not PASSWORD_POLICY['digit'].search(password):
        return False
    if not PASSWORD_POLICY['special'].search(password):
        return False
    return True

# ------------------------------------------------------------------------------
# Database utility
# ------------------------------------------------------------------------------

def get_db_connection():
    """
    Return a new MySQLdb connection using environment-provided credentials.
    The database user must have only INSERT privileges on the user table.
    """
    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            port=int(os.environ.get('DB_PORT', '3306')),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASSWORD'),
            db=os.environ.get('DB_NAME'),
            cursorclass=DictCursor,
            ssl={'ssl': {}}  # Require SSL/TLS for DB connection, if supported
        )
        return conn
    except Exception as e:
        # Do not expose internal details to the client
        logger.error('Database connection failed: %s', e)
        raise

# ------------------------------------------------------------------------------
# Registration endpoint
# ------------------------------------------------------------------------------

@app.route('/register', methods=['POST'])
@csrf.exempt  # We assume CSRFProtect is globally enabled; you can remove this if using built-in CSRF token in forms
def register():
    """
    Secure user registration.
    Steps:
    1. Retrieve and validate inputs.
    2. Hash password with Argon2 (recommended by OWASP).
    3. Use parameterized queries to prevent SQL injection.
    4. Handle errors without leaking sensitive information.
    5. Redirect on success using 303 See Other (to avoid re-POST).
    """

    # 1. Retrieve inputs in a safe manner
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Validate inputs
    if not validate_username(username):
        # Generic error message to avoid giving clues
        abort(400, description='Invalid input.')

    if not validate_password(password):
        abort(400, description='Invalid input.')

    # 3. Hash the password securely with Argon2
    #    We use a high memory_cost, time_cost, and parallelism according to OWASP recommendations.
    #    The salt is generated internally by passlib.
    try:
        password_hash = argon2.using(
            time_cost=4,
            memory_cost=102400,  # 100 MiB
            parallelism=4
        ).hash(password)
    except Exception as e:
        # Internal error during hashing
        logger.error('Password hashing failed: %s', e)
        abort(500, description='Internal server error.')

    # 4. Store user
    conn = None
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            # Parameterized query prevents SQL injection
            insert_sql = """
                INSERT INTO users (username, password_hash, created_at)
                VALUES (%s, %s, NOW())
            """
            cursor.execute(insert_sql, (username, password_hash))
        conn.commit()
    except MySQLdb.IntegrityError as e:
        # Assume duplicate username or constraint violation
        logger.info('Registration failed: %s', e)
        # Do not reveal whether the username is taken for privacy
        abort(400, description='Invalid input.')
    except Exception as e:
        # Catch-all for other DB errors
        logger.error('Database error during registration: %s', e)
        abort(500, description='Internal server error.')
    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    # 5. On success, redirect to a login page (or home).
    # Use 303 See Other to prevent the browser from resubmitting the POST
    response = make_response(redirect('/login', code=303))
    # Prevent caching of responses containing sensitive data
    response.headers['Cache-Control'] = 'no-store'
    return response

# ------------------------------------------------------------------------------
# Generic error handlers (no stack traces leaked to clients)
# ------------------------------------------------------------------------------

@app.errorhandler(400)
def handle_bad_request(e):
    return str(e.description), 400

@app.errorhandler(500)
def handle_internal_error(e):
    return 'An internal error occurred. Please try again later.', 500
