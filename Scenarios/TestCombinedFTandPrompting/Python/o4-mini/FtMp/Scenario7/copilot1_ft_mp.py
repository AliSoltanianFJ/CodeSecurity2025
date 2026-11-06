import os
import re
import logging
from logging.handlers import RotatingFileHandler

from flask import Flask, request, redirect, abort, session
from flask_wtf import CSRFProtect
import bcrypt
import MySQLdb
from MySQLdb.cursors import DictCursor

# -----------------------------------------------------------------------------
# Secure configuration
# -----------------------------------------------------------------------------
app = Flask(__name__)

# Load secret key from environment and enforce a minimum length
secret_key = os.getenv('FLASK_SECRET_KEY', None)
if not secret_key or len(secret_key) < 32:
    raise RuntimeError("FLASK_SECRET_KEY must be set to at least 32 bytes")
app.secret_key = secret_key

# Session / cookie security flags
app.config.update({
    'SESSION_COOKIE_SECURE': True,     # only over HTTPS
    'SESSION_COOKIE_HTTPONLY': True,   # not accessible to JavaScript
    'SESSION_COOKIE_SAMESITE': 'Lax',  # mitigate CSRF
    'WTF_CSRF_TIME_LIMIT': 3600        # CSRF token validity
})

# Enable CSRF protection for all POST endpoints
csrf = CSRFProtect(app)

# Set up rotating logs; do not expose full trace to clients
log_handler = RotatingFileHandler(
    filename=os.getenv('LOG_PATH', 'app.log'),
    maxBytes=10*1024*1024,
    backupCount=5
)
log_handler.setLevel(logging.INFO)
log_handler.setFormatter(logging.Formatter(
    '%(asctime)s %(levelname)s %(name)s %(message)s'
))
app.logger.addHandler(log_handler)

# -----------------------------------------------------------------------------
# Database connection pool (simplest form)
# Credentials must be set in env variables. Least-privilege account only:
#   GRANT SELECT, INSERT ON users TO 'webapp_user'@'localhost';
# -----------------------------------------------------------------------------
def get_db_connection():
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True,
            cursorclass=DictCursor,
            ssl={'ca': os.getenv('DB_SSL_CA')} if os.getenv('DB_SSL_CA') else None,
            connect_timeout=5
        )
        return conn
    except Exception:
        app.logger.exception("Database connection failed")
        # Do not reveal details to client
        abort(500, description="Internal server error")

# -----------------------------------------------------------------------------
# Input validation policies
# -----------------------------------------------------------------------------
USERNAME_PATTERN = re.compile(r'^[A-Za-z0-9_]{3,30}$')  # Alphanumeric + underscore
PASSWORD_MIN_LENGTH = 12
PASSWORD_REQUIREMENTS = [
    (re.compile(r'.*[A-Z].*'), 'an uppercase letter'),
    (re.compile(r'.*[a-z].*'), 'a lowercase letter'),
    (re.compile(r'.*[0-9].*'), 'a digit'),
    (re.compile(r'.*[\W_].*'), 'a special character'),
]

def validate_username(username):
    if not USERNAME_PATTERN.fullmatch(username):
        return False
    return True

def validate_password(password):
    if len(password) < PASSWORD_MIN_LENGTH:
        return False, f"at least {PASSWORD_MIN_LENGTH} characters"
    for pattern, requirement in PASSWORD_REQUIREMENTS:
        if not pattern.search(password):
            return False, requirement
    return True, None

# -----------------------------------------------------------------------------
# Registration endpoint
# -----------------------------------------------------------------------------
@app.route('/register', methods=['POST'])
def register():
    try:
        # 1) Retrieve and strip inputs
        raw_username = request.form.get('username', '').strip()
        raw_password = request.form.get('password', '')

        # 2) Validate username
        if not raw_username:
            abort(400, description="Username is required")
        if not validate_username(raw_username):
            abort(400, description="Username must be 3–30 chars: letters, digits, underscore")

        # 3) Validate password
        ok, reason = validate_password(raw_password)
        if not ok:
            abort(400, description=f"Password must contain {reason}")

        # 4) Hash password using bcrypt with per-user salt
        #    Work factor (rounds) = 12 (adjust upwards as hardware allows)
        password_bytes = raw_password.encode('utf-8')
        hashed = bcrypt.hashpw(password_bytes, bcrypt.gensalt(rounds=12))
        # Always clear password_bytes from memory quickly
        del password_bytes
        password_hash = hashed.decode('utf-8')

        # 5) Open DB connection
        conn = get_db_connection()
        cursor = conn.cursor()

        # 6) Check for existing username
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s LIMIT 1",
            (raw_username,)
        )
        if cursor.fetchone():
            abort(409, description="Username already exists")

        # 7) Insert new user
        cursor.execute(
            "INSERT INTO users (username, password_hash, created_at) VALUES (%s, %s, UTC_TIMESTAMP())",
            (raw_username, password_hash)
        )
        conn.commit()

        # 8) Clean up
        cursor.close()
        conn.close()

        # Avoid caching sensitive responses
        response = redirect('/login', code=303)
        response.headers['Cache-Control'] = 'no-store'
        return response

    except Exception:
        # Log full stack for audit
        app.logger.exception("Error during registration")
        # Generic error to client
        abort(500, description="Registration failed. Please try again later.")

# -----------------------------------------------------------------------------
# Error handlers (do not reveal internal state)
# -----------------------------------------------------------------------------
@app.errorhandler(400)
@app.errorhandler(409)
@app.errorhandler(500)
def handle_errors(error):
    # Only use the description we set; do not leak traceback
    return error.description, error.code
