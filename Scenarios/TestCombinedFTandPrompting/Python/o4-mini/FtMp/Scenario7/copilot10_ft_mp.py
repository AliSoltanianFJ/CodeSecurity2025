import os
import re
import logging

from flask import Flask, request, redirect, url_for, abort, session
from flask_wtf import CSRFProtect
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from werkzeug.security import generate_password_hash
import MySQLdb

# ------------------------------------------------------------------------------
# Application Setup and Secure Defaults
# ------------------------------------------------------------------------------

app = Flask(__name__)

# 1. Secret key for sessions and CSRF. Must be set in environment, no defaults.
app.config['SECRET_KEY'] = os.getenv('SECRET_KEY', None)
if not app.config['SECRET_KEY']:
    raise RuntimeError("Fatal: SECRET_KEY environment variable is not set")

# 2. Enforce secure session cookie flags
app.config['SESSION_COOKIE_SECURE'] = True          # only sent over HTTPS
app.config['SESSION_COOKIE_HTTPONLY'] = True        # not accessible via JavaScript
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'       # prevent CSRF in some contexts

# 3. Initialize CSRF protection (prevents CSRF)
csrf = CSRFProtect(app)

# 4. Rate limiting (prevent brute-force / DoS)
limiter = Limiter(
    app,
    key_func=get_remote_address,
    default_limits=["5 per minute"]  # adjust as appropriate
)

# 5. Configure logging: do not log sensitive data
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s %(levelname)s [%(name)s] %(message)s'
)
logger = logging.getLogger("register")

# 6. Database credentials via environment; least-privilege account (only INSERT)
DB_CONFIG = {
    'host':     os.getenv('DB_HOST'),
    'user':     os.getenv('DB_USER'),
    'passwd':   os.getenv('DB_PASSWORD'),
    'db':       os.getenv('DB_NAME'),
    'charset':  'utf8mb4',
    'use_unicode': True,
    'client_flag': MySQLdb.CLIENT.MULTI_STATEMENTS ^ MySQLdb.CLIENT.SECURE_CONNECTION
}
if not all(DB_CONFIG.values()):
    raise RuntimeError("Fatal: Database configuration environment variables are not fully set")

# ------------------------------------------------------------------------------
# Helper Functions
# ------------------------------------------------------------------------------

def get_db_connection():
    """
    Returns a new database connection using secure parameters.
    Caller is responsible for closing the connection.
    """
    conn = MySQLdb.connect(**DB_CONFIG)
    # Ensure autocommit is off; we will commit explicitly to detect failures.
    conn.autocommit(False)
    return conn

def is_valid_username(u):
    """
    Username policy:
      - Length between 3 and 30
      - Letters, digits, underscore, hyphen only
    """
    return bool(re.fullmatch(r'[A-Za-z0-9_-]{3,30}', u))

def is_valid_password(p):
    """
    Password policy:
      - Length between 8 and 128
      - At least one uppercase, one lowercase, one digit, one symbol
    """
    if not 8 <= len(p) <= 128:
        return False
    # Each lookahead ensures presence of at least one category
    patterns = [r'.*[A-Z].*', r'.*[a-z].*', r'.*[0-9].*', r'.*[\W_].*']
    return all(re.search(pat, p) for pat in patterns)


# ------------------------------------------------------------------------------
# Registration Endpoint
# ------------------------------------------------------------------------------

@app.route('/register', methods=['POST'])
@limiter.limit("3 per hour")  # Stricter limit on account creation
def register():
    try:
        # 1. Retrieve and validate inputs
        username = request.form.get('username', '').strip()
        password = request.form.get('password', '')

        if not username or not password:
            # Generic error; do not reveal which field was missing
            abort(400, description="Invalid input.")

        if not is_valid_username(username):
            abort(400, description="Invalid username or password.")  # generic

        if not is_valid_password(password):
            abort(400, description="Invalid username or password.")  # generic

        # 2. Hash the password using PBKDF2-SHA256 with per-user salt
        # werkzeug.security.generate_password_hash uses a safe salt by default
        password_hash = generate_password_hash(
            password,
            method='pbkdf2:sha256',
            salt_length=16
        )
        # Clear plaintext password from memory as soon as possible
        password = None

        # 3. Insert into database using parameterized query
        conn = get_db_connection()
        cursor = conn.cursor()
        insert_sql = """
            INSERT INTO users (username, password_hash, created_at)
            VALUES (%s, %s, UTC_TIMESTAMP())
        """
        try:
            cursor.execute(insert_sql, (username, password_hash))
            conn.commit()
        except MySQLdb.IntegrityError as e:
            conn.rollback()
            # Duplicate username or constraint failure
            logger.info("Registration attempt with existing username")
            # Generic response to prevent user enumeration
            abort(400, description="Registration failed.")
        except Exception as e:
            conn.rollback()
            logger.error("Database error during registration for user")
            # Do not reveal internal details
            abort(500, description="Registration failed.")
        finally:
            cursor.close()
            conn.close()

        # 4. On success, redirect to login page (use relative URLs)
        return redirect(url_for('login'), code=303)

    except Exception as e:
        # If abort() was called, Flask will handle it.
        if hasattr(e, 'code') and (400 <= getattr(e, 'code', 0) < 600):
            raise  # re-raise HTTPException
        # Catch-all for unexpected errors
        logger.error(f"Unexpected error in register(): {e}")
        abort(500, description="Registration failed.")

# Placeholder login endpoint for redirect
@app.route('/login', methods=['GET'])
def login():
    # Render login form (implementation not shown)
    return "Login Page"
