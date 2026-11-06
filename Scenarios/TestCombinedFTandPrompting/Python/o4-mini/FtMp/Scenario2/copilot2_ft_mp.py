import os
import logging
from datetime import datetime
from functools import wraps

from flask import Flask, request, abort, Response
from flask_talisman import Talisman
from email_validator import validate_email, EmailNotValidError
from itsdangerous import URLSafeTimedSerializer, BadData
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
import MySQLdb

# ------------------------------------------------------------
# Configuration Loading and Validation
# ------------------------------------------------------------

# Required environment variables (do NOT hardcode in source)
REQUIRED_ENVS = [
    "SECRET_KEY",         # Flask secret key for session & token signing
    "UNSUBSCRIBE_SALT",   # Salt for signing unsubscribe tokens
    "DB_HOST",            # MySQL host
    "DB_PORT",            # MySQL port
    "DB_NAME",            # MySQL database name
    "DB_USER",            # MySQL user (least privilege)
    "DB_PASSWORD",        # MySQL password
    "DB_SSL_CA_PATH",     # Path to CA cert for MySQL SSL
]

missing = [e for e in REQUIRED_ENVS if not os.getenv(e)]
if missing:
    raise RuntimeError(f"Missing required environment variables: {', '.join(missing)}")

# Parse numeric port
try:
    DB_PORT = int(os.getenv("DB_PORT"))
except ValueError:
    raise RuntimeError("DB_PORT must be an integer")

# Flask application
app = Flask(__name__)
app.config["SECRET_KEY"] = os.getenv("SECRET_KEY")
app.config["SESSION_COOKIE_SECURE"] = True        # Cookies only sent on HTTPS
app.config["SESSION_COOKIE_HTTPONLY"] = True      # Prevent JavaScript access
app.config["SESSION_COOKIE_SAMESITE"] = "Lax"     # CSRF mitigation

# Enforce HTTPS, HSTS, CSP, X-Frame-Options, etc.
Talisman(
    app,
    content_security_policy={
        "default-src": ["'none'"],
        "script-src": ["'none'"],
        "style-src": ["'none'"],
        "img-src": ["'none'"],
    },
    force_https=True,
    strict_transport_security=True,
    strict_transport_security_max_age=31536000,  # 1 year
)

# Rate limiter to prevent brute-forcing and enumeration
limiter = Limiter(
    app,
    key_func=get_remote_address,
    default_limits=["10 per minute"],
    headers_enabled=True
)

# Serializer for signed, timestamped unsubscribe tokens
serializer = URLSafeTimedSerializer(
    secret_key=app.config["SECRET_KEY"],
    salt=os.getenv("UNSUBSCRIBE_SALT")
)

# Set up structured logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger(__name__)

# ------------------------------------------------------------
# Helper Functions
# ------------------------------------------------------------

def get_db_connection():
    """
    Establish a secure, SSL-encrypted connection to the MySQL database
    using a least-privilege account with a short connection timeout.
    """
    try:
        conn = MySQLdb.connect(
            host=os.getenv("DB_HOST"),
            port=DB_PORT,
            user=os.getenv("DB_USER"),
            passwd=os.getenv("DB_PASSWORD"),
            db=os.getenv("DB_NAME"),
            ssl={
                "ca": os.getenv("DB_SSL_CA_PATH")
            },
            connect_timeout=5,
            read_timeout=5,
            write_timeout=5,
            charset="utf8mb4",
            use_unicode=True,
            autocommit=False
        )
        return conn
    except MySQLdb.MySQLError as e:
        # Do not expose internal errors to users
        logger.critical("Database connection failed: %s", e)
        abort(500)

def require_parameters(*params):
    """
    Decorator to ensure required GET parameters are present.
    Returns HTTP 400 if any are missing.
    """
    def decorator(f):
        @wraps(f)
        def wrapped(*args, **kwargs):
            for p in params:
                if not request.args.get(p, type=str):
                    abort(400)
            return f(*args, **kwargs)
        return wrapped
    return decorator

def validate_and_normalize_email(raw_email):
    """
    Validate email syntax, length, and normalize it.
    Raises HTTP 400 on validation failure.
    """
    try:
        # email_validator checks RFC compliance and length limits
        v = validate_email(raw_email, check_deliverability=False)
        return v.email
    except EmailNotValidError as e:
        abort(400)

def verify_unsubscribe_token(token, email):
    """
    Verify that the token matches the email and is not expired (max_age = 86400s).
    Raises HTTP 400 on failure.
    """
    try:
        data = serializer.loads(token, max_age=86400)  # 24h expiry
        if data != email:
            logger.warning("Token/email mismatch: %s != %s", data, email)
            abort(400)
    except BadData:
        abort(400)

# ------------------------------------------------------------
# Error Handlers
# ------------------------------------------------------------

@app.errorhandler(400)
def handle_bad_request(e):
    # Generic 400 response without details to avoid information leakage
    return Response("Bad request.", status=400, mimetype="text/plain; charset=utf-8")

@app.errorhandler(429)
def handle_rate_limit(e):
    # Generic 429 response
    return Response("Too many requests. Please try again later.", status=429,
                    mimetype="text/plain; charset=utf-8")

@app.errorhandler(500)
def handle_server_error(e):
    # Generic 500 response without stack trace
    return Response("An internal error occurred.", status=500,
                    mimetype="text/plain; charset=utf-8")

# ------------------------------------------------------------
# Unsubscribe Endpoint
# ------------------------------------------------------------

@app.route("/unsubscribe", methods=["GET"])
@limiter.limit("5 per hour")               # stricter per-endpoint limit
@require_parameters("email", "token")
def unsubscribe():
    """
    Secure unsubscribe endpoint:
    - GET parameters: email, token
    - token is a time-limited signature of the email
    - Does not reveal whether the email existed (prevents enumeration)
    - Uses parameterized queries to avoid SQL injection
    - Commits only if an UPDATE occurred
    """
    remote_ip = request.remote_addr
    raw_email = request.args.get("email")
    token = request.args.get("token")

    # 1. Validate and normalize email
    email = validate_and_normalize_email(raw_email)

    # 2. Verify unsubscribe token integrity and expiry
    verify_unsubscribe_token(token, email)

    # 3. Perform database update
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            # Parameterized query to prevent SQL injection
            sql = """
                UPDATE subscriptions
                   SET active = FALSE,
                       unsubscribed_at = %s
                 WHERE email = %s
                   AND active = TRUE
            """
            now = datetime.utcnow().replace(microsecond=0)
            cursor.execute(sql, (now, email))
            rows_affected = cursor.rowcount
        # Commit only if the query succeeded
        conn.commit()
    except MySQLdb.MySQLError as e:
        conn.rollback()
        abort(500)
    finally:
        conn.close()

    # 4. Generic response (never reveals if email was present)
    return Response(
        "If your email was in our list, you have been unsubscribed.",
        status=200,
        mimetype="text/plain; charset=utf-8"
    )

# ------------------------------------------------------------
# Token Generation Utility (for emails sent to users)
# ------------------------------------------------------------

def generate_unsubscribe_token(email):
    """
    Given a validated, normalized email, generate a time-limited token.
    This function would be used when sending emails with unsubscribe links.
    """
    return serializer.dumps(email)
