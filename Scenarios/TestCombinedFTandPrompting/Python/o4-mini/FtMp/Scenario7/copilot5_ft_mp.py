import os
import re
import logging
import base64
import hashlib
import MySQLdb
from flask import Flask, request, redirect, abort, render_template_string
from flask_wtf import CSRFProtect
from flask_talisman import Talisman
from mysql.connector.constants import ClientFlag
from MySQLdb import IntegrityError

# ------------------------------------------------------------------------------
# App factory / configuration
# ------------------------------------------------------------------------------

app = Flask(__name__)

# 1) Secure configuration by defaults
app.config.update(
    # SECRET_KEY used for session signing and CSRF; MUST be long, random, and kept secret
    SECRET_KEY=os.environ.get('FLASK_SECRET_KEY'),
    # Enforce session cookies are only sent over HTTPS and not accessible to JS
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE='Lax',
    # WTF-CSRF
    WTF_CSRF_TIME_LIMIT=3600,            # 1h token lifetime
    WTF_CSRF_FIELD_NAME='csrf_token',
)

if not app.config['SECRET_KEY']:
    raise RuntimeError("FLASK_SECRET_KEY environment variable is required and must be a strong random value")

# 2) Enforce HTTPS, HSTS, and sane CSP defaults
#    This prevents downgrade attacks, cookie theft, and many injection classes.
Talisman(
    app,
    content_security_policy={
        'default-src': ["'self'"],
        'script-src': ["'self'"],
        'style-src': ["'self'"],
    },
    force_https=True,
    strict_transport_security=True,
    strict_transport_security_max_age=31536000,  # 1 year
    strict_transport_security_include_subdomains=True
)

# 3) CSRF protection for all POST, PUT, DELETE, PATCH requests
csrf = CSRFProtect(app)

# 4) Application logging (do NOT log sensitive data)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s %(levelname)s %(name)s %(message)s'
)
logger = logging.getLogger(__name__)


# ------------------------------------------------------------------------------
# Database connection factory
# ------------------------------------------------------------------------------

def get_db_connection():
    """
    Establish a new database connection on each request.
    Uses environment variables for credentials and enforces SSL.
    Applies least-privilege: the 'app_user' only has INSERT and SELECT on users table.
    """
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    ssl_ca = os.environ.get('DB_SSL_CA')  # Path to CA certificate

    if not all([db_host, db_user, db_pass, db_name, ssl_ca]):
        logger.critical("Database configuration incomplete")
        raise RuntimeError("Database configuration incomplete")

    # MySQLdb does not support mysql_native_password well over SSL in all versions;
    # if necessary switch to mysql-connector/python or PyMySQL.

    conn = MySQLdb.connect(
        host=db_host,
        user=db_user,
        passwd=db_pass,
        db=db_name,
        ssl={'ca': ssl_ca},
        charset='utf8mb4',                  # Prevent encoding issues / unicode truncation
        use_unicode=True,
        connect_timeout=5
    )
    # Ensure we have autocommit off; we explicitly commit or rollback.
    conn.autocommit(False)
    return conn


# ------------------------------------------------------------------------------
# Utilities
# ------------------------------------------------------------------------------

def validate_username(username):
    """
    - Length between 3 and 30.
    - Only allow alphanumeric plus underscore.
    """
    if not 3 <= len(username) <= 30:
        return False
    # Entire string must match pattern
    return re.fullmatch(r'[A-Za-z0-9_]+', username) is not None

def validate_password(password):
    """
    - Minimum length 12.
    - Must include at least one uppercase, one lowercase, one digit, one special char.
    """
    if len(password) < 12:
        return False
    # Positive lookahead for each class
    patterns = [
        r'.*[A-Z].*',
        r'.*[a-z].*',
        r'.*[0-9].*',
        r'.*[\W_].*'
    ]
    return all(re.search(p, password) for p in patterns)

def hash_password(password):
    """
    Uses PBKDF2-HMAC-SHA256 with 200,000 iterations and a 16-byte random salt.
    Returns (salt_base64, hash_base64). Both are safe to store.
    """
    salt = os.urandom(16)
    # 200k iterations balances security and performance
    dk = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        iterations=200_000
    )
    # Use URL-safe base64 to avoid padding issues
    return (
        base64.urlsafe_b64encode(salt).decode('ascii'),
        base64.urlsafe_b64encode(dk).decode('ascii')
    )


# ------------------------------------------------------------------------------
# Routes
# ------------------------------------------------------------------------------

@app.route('/register', methods=['GET'])
def register_form():
    """
    Simple registration form. In a real app, use templates.
    CSRF token is injected automatically by Flask-WTF.
    """
    form_html = """
    <form method="post" action="/register">
      {{ csrf_token() }}
      <label>Username:<input name="username" type="text" required></label><br>
      <label>Password:<input name="password" type="password" required></label><br>
      <button type="submit">Register</button>
    </form>
    """
    return render_template_string(form_html)


@app.route('/register', methods=['POST'])
def register():
    """
    Secure user registration endpoint.

    Security controls:
    - CSRFProtect: blocks CSRF.
    - Input validation: strict allow-list.
    - Rate limiting: should be placed in front (e.g. nginx, Flask-Limiter).
    - Strong password hashing.
    - Parameterized queries only.
    - Generic error messages to avoid user enumeration.
    - HTTPS enforced by Talisman.
    """
    try:
        # 1) Retrieve and strip inputs
        username = request.form.get('username', '').strip()
        password = request.form.get('password', '')

        # 2) Validate inputs
        if not validate_username(username) or not validate_password(password):
            # Generic failure, do not reveal which check failed
            abort(400, description="Invalid username or password criteria.")

        # 3) Hash password
        salt_b64, hash_b64 = hash_password(password)

        # 4) Store in database
        conn = get_db_connection()
        cursor = conn.cursor()

        # parameterized INSERT prevents SQL injection
        insert_sql = """
          INSERT INTO users (username, password_hash, salt)
          VALUES (%s, %s, %s)
        """
        cursor.execute(insert_sql, (username, hash_b64, salt_b64))
        conn.commit()

    except IntegrityError as e:
        # Likely a duplicate username. Generic message to avoid enumeration.
        logger.info("Attempt to register existing username")
        # Roll back in case partial work has happened
        try:
            conn.rollback()
        except Exception:
            pass
        abort(409, description="Registration failed. Please choose a different username or try again.")

    except Exception as e:
        # Log full exception server-side for auditing.
        logger.exception("Unexpected error during registration")
        # Ensure no sensitive data is leaked to client
        abort(500, description="An internal error occurred. Please try again later.")

    finally:
        # Clean up database handles
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

    # 5) On success, redirect to login (avoid revealing additional info)
    return redirect('/login', code=302)


# ------------------------------------------------------------------------------
# Error handlers (no stack traces to users)
# ------------------------------------------------------------------------------
@app.errorhandler(400)
@app.errorhandler(409)
@app.errorhandler(500)
def handle_errors(error):
    """
    Return minimal JSON or HTML without sensitive details.
    """
    return (
        # In a JSON API you might return jsonify({ "error": error.description }),
        f"<h1>Error</h1><p>{error.description}</p>",
        error.code
    )
