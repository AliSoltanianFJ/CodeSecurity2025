import os
import re
import logging
import logging.handlers
from flask import Flask, request, redirect, abort
from flask_wtf import CSRFProtect
import MySQLdb
import MySQLdb.cursors
from passlib.hash import argon2

# ------------------------------------------------------------------------------
# Application Initialization and Secure Configuration
# ------------------------------------------------------------------------------

app = Flask(__name__)

# Secure default configurations
# - Disable debugging and testing in production
# - Enforce HTTPS-only cookies with HTTPOnly and SameSite flags
# - Limit maximum request size to mitigate DoS via large payloads
# - Require a strong, randomly generated SECRET_KEY supplied via environment
app.config.update({
    'DEBUG': False,
    'TESTING': False,
    'SESSION_COOKIE_SECURE': True,
    'SESSION_COOKIE_HTTPONLY': True,
    'SESSION_COOKIE_SAMESITE': 'Lax',
    'PREFERRED_URL_SCHEME': 'https',
    'MAX_CONTENT_LENGTH': 1024,  # bytes; registration payload must be small
    # SECRET_KEY must be set in the environment and kept secret
    'SECRET_KEY': os.environ['FLASK_SECRET_KEY'],
    # CSRF token lifetime (None = do not expire tokens quickly)
    'WTF_CSRF_TIME_LIMIT': None
})

# Initialize CSRF protection from flask-wtf
# Ensures all POST endpoints require a valid CSRF token
csrf = CSRFProtect(app)

# ------------------------------------------------------------------------------
# Logging Configuration
# ------------------------------------------------------------------------------

logger = logging.getLogger('secure_registration')
logger.setLevel(logging.INFO)
handler = logging.handlers.RotatingFileHandler(
    filename=os.environ.get('LOG_FILE_PATH', '/var/log/app_registration.log'),
    maxBytes=10 * 1024 * 1024,  # 10 MB per file
    backupCount=5
)
handler.setFormatter(logging.Formatter(
    '%(asctime)s %(levelname)s %(name)s:%(lineno)d %(message)s'
))
logger.addHandler(handler)

# ------------------------------------------------------------------------------
# Helper: Database Connection Factory
# ------------------------------------------------------------------------------

def get_db_connection():
    """
    Establishes a secure, least-privilege connection to the MySQL database
    using only environment-derived credentials. Enforces SSL and a short
    connect timeout.
    """
    try:
        conn = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            port=int(os.environ.get('DB_PORT', '3306')),
            user=os.environ['DB_USER'],           # least-privileged user
            passwd=os.environ['DB_PASSWORD'],     # do NOT hardcode secrets
            db=os.environ['DB_NAME'],
            ssl={'ca': os.environ['DB_SSL_CA']},  # verify server certificate
            cursorclass=MySQLdb.cursors.DictCursor,
            connect_timeout=5
        )
        return conn
    except KeyError as e:
        # Missing required environment configuration
        logger.error("Database configuration missing key: %s", e)
        raise
    except Exception:
        # Log full stack for internal audit; do not expose details to users
        logger.exception("Failed to establish database connection")
        raise

# ------------------------------------------------------------------------------
# Registration Endpoint
# ------------------------------------------------------------------------------

@app.route('/register', methods=['POST'])
def register():
    """
    Secure user registration:
    - Validates and sanitizes all inputs (whitelisting).
    - Enforces strong password complexity.
    - Uses Argon2 for password hashing with per-user salt.
    - Inserts user via parameterized query to prevent SQL injection.
    - Handles errors without leaking sensitive information.
    - Redirects on success; returns generic errors otherwise.
    """

    # Enforce maximum payload size (configured via MAX_CONTENT_LENGTH)
    # Flask will automatically return 413 if content is too large.

    # Retrieve and trim inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Input Validation: username must be 3–30 chars, alphanumeric only
    if not re.fullmatch(r'[A-Za-z0-9]{3,30}', username):
        # Do not reveal which field failed; generic error message
        return "Invalid username or password", 400

    # Password Complexity: length >=12, includes upper, lower, digit, special
    if (len(password) < 12
        or not re.search(r'[A-Z]', password)
        or not re.search(r'[a-z]', password)
        or not re.search(r'\d', password)
        or not re.search(r'\W', password)):
        return "Invalid username or password", 400

    # Hash password with Argon2 (memory-hard function)
    # Parameters chosen to balance security and performance; adjust as needed
    try:
        password_hash = argon2.using(
            time_cost=2,
            memory_cost=102400,   # 100 MiB
            parallelism=8
        ).hash(password)
    except Exception:
        logger.exception("Password hashing failed")
        # On hashing failure, abort with server error
        abort(500)
    finally:
        # Remove plaintext password from memory as soon as possible
        try:
            del password
        except Exception:
            pass

    # Database insertion
    conn = None
    cursor = None
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Parameterized query prevents SQL injection
        insert_sql = """
            INSERT INTO users (username, password_hash)
            VALUES (%s, %s)
        """
        cursor.execute(insert_sql, (username, password_hash))
        conn.commit()

    except MySQLdb.IntegrityError as e:
        # MySQL error code 1062 = duplicate entry
        if e.args and e.args[0] == 1062:
            # Username already exists
            return "Username already exists", 400
        else:
            logger.exception("Database integrity error during registration")
            abort(500)

    except Exception:
        # Catch all unexpected errors
        logger.exception("Unexpected error during user registration")
        abort(500)

    finally:
        # Clean up database resources
        if cursor:
            try:
                cursor.close()
            except Exception:
                logger.exception("Failed to close database cursor")
        if conn:
            try:
                conn.close()
            except Exception:
                logger.exception("Failed to close database connection")

    # On success, redirect to login page using safe HTTP status code 303
    return redirect('/login', code=303)
