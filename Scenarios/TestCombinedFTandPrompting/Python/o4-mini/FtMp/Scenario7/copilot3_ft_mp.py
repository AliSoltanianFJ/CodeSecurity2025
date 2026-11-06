import os
import re
import logging
from flask import Flask, request, jsonify, abort
from flask_wtf import CSRFProtect
from mysql.connector import connect, Error as MySQLError
from argon2 import PasswordHasher, exceptions as argon2_exceptions
from werkzeug.middleware.proxy_fix import ProxyFix

# ------------------------------------------------------------------------------
# Configuration & Initialization
# ------------------------------------------------------------------------------

# Ensure the following env vars are set; fail fast if missing.
required_env = [
    'APP_SECRET_KEY',
    'DB_HOST', 'DB_PORT', 'DB_USER', 'DB_PASSWORD', 'DB_NAME',
    'MYSQL_SSL_CA'
]
missing = [v for v in required_env if not os.getenv(v)]
if missing:
    raise RuntimeError(f"Missing required environment variables: {', '.join(missing)}")

app = Flask(__name__)

# Secure secret key for sessions and CSRF; do not hardcode
app.config['SECRET_KEY'] = os.getenv('APP_SECRET_KEY')

# Enforce HTTPS (if behind a proxy/load-balancer terminating TLS)
# and trust only one hop (adjust as needed).
app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1)
app.config['SESSION_COOKIE_SECURE'] = True      # only send cookie over HTTPS
app.config['REMEMBER_COOKIE_SECURE'] = True
app.config['SESSION_COOKIE_HTTPONLY'] = True    # mitigate JavaScript access
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'

# Enable CSRF protection
csrf = CSRFProtect(app)

# Configure logging: do not log sensitive data
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s %(levelname)s %(message)s'
)
logger = logging.getLogger(__name__)

# Argon2 parameters: tuned per your threat model and hardware
ph = PasswordHasher(
    time_cost=3,       # number of iterations
    memory_cost=64 * 1024,  # 64 MiB
    parallelism=4,
    hash_len=32,
    salt_len=16
)

# Precompile regex for username validation
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,30}$')

# Password policy regex (at least 12 chars, an upper, a lower, a digit, a special)
PASSWORD_POLICY_REGEX = re.compile(
    r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_\-+=\[\]{}|;:,.<>/?]).{12,}$'
)

# ------------------------------------------------------------------------------
# Helper: sanitize and validate inputs
# ------------------------------------------------------------------------------

def validate_username(username):
    """Return True if username passes policy; else False."""
    return bool(USERNAME_REGEX.fullmatch(username))

def validate_password(password):
    """Return True if password passes strength policy; else False."""
    return bool(PASSWORD_POLICY_REGEX.fullmatch(password))

# ------------------------------------------------------------------------------
# /register endpoint
# ------------------------------------------------------------------------------

@app.route('/register', methods=['POST'])
def register():
    try:
        # 1. Extract and trim inputs
        username = request.form.get('username', '').strip()
        password = request.form.get('password', '')
        # CSRF token is auto-checked by flask-wtf

        # 2. Validate inputs
        if not username or not password:
            # Do not reveal which field failed
            abort(400, description="Invalid request parameters.")

        if not validate_username(username):
            abort(400, description="Invalid username or password.")  # generic message

        if not validate_password(password):
            abort(400, description="Invalid username or password.")  # generic message

        # 3. Hash the password using Argon2
        #    The PasswordHasher automatically generates a random salt.
        try:
            password_hash = ph.hash(password)
        except Exception:
            # Catch any hashing error without revealing details
            logger.exception("Password hashing failed.")
            abort(500, description="Internal server error.")

        # 4. Insert into database with least-privileged credentials
        #    Use TLS for the connection and parameterized queries to prevent SQLi.
        conn = connect(
            host=os.getenv('DB_HOST'),
            port=int(os.getenv('DB_PORT')),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            database=os.getenv('DB_NAME'),
            ssl_ca=os.getenv('MYSQL_SSL_CA'),
            ssl_verify_cert=True,
            connection_timeout=5,
            auth_plugin='mysql_native_password'
        )
        cursor = conn.cursor(prepared=True)

        # 5. Check for existing username (to prevent duplicate)
        check_sql = "SELECT 1 FROM users WHERE username = %s LIMIT 1"
        cursor.execute(check_sql, (username,))
        if cursor.fetchone():
            # Do not reveal that username exists
            abort(409, description="User registration failed.")

        # 6. Insert new user
        insert_sql = (
            "INSERT INTO users (username, password_hash, created_at) "
            "VALUES (%s, %s, UTC_TIMESTAMP())"
        )
        cursor.execute(insert_sql, (username, password_hash))
        conn.commit()

        # 7. Close resources
        cursor.close()
        conn.close()

        # 8. Respond with success
        return jsonify(message="Registration successful."), 201

    except MySQLError:
        # Log detailed error server-side, return generic message
        logger.exception("Database error during registration.")
        abort(500, description="Internal server error.")

    except argon2_exceptions.HashingError:
        # Rare: problems during hashing
        logger.exception("Argon2 hashing exception.")
        abort(500, description="Internal server error.")

    except Exception:
        # Catch-all for unexpected errors
        logger.exception("Unexpected error in registration endpoint.")
        abort(500, description="Internal server error.")

# ------------------------------------------------------------------------------
# Error handlers
# ------------------------------------------------------------------------------

@app.errorhandler(400)
@app.errorhandler(409)
@app.errorhandler(500)
def handle_errors(error):
    """
    Return JSON errors without leaking stack traces or internal details.
    CSRF failures from flask-wtf also surface here with code=400.
    """
    response = jsonify(message=error.description or "An error occurred.")
    response.status_code = error.code if hasattr(error, 'code') else 500
    return response
