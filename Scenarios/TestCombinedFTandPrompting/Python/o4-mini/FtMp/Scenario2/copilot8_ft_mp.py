import os
import re
import logging
from datetime import timedelta

from flask import Flask, request, abort, redirect, make_response
from itsdangerous import URLSafeTimedSerializer, BadSignature, SignatureExpired
import MySQLdb

# -----------------------------------------------------------------------------
# Configuration and Initialization
# -----------------------------------------------------------------------------

# Configure logging (avoid sensitive data in logs)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger(__name__)

# Load and validate critical configuration from environment
required_env = ["SECRET_KEY", "DB_HOST", "DB_USER", "DB_PASSWORD", "DB_NAME"]
for var in required_env:
    if not os.environ.get(var):
        logger.critical("Missing required environment variable: %s", var)
        # Fail fast on startup if config is incomplete
        raise RuntimeError(f"Configuration error: {var} not set")

# Token settings
SECRET_KEY = os.environ["SECRET_KEY"]
TOKEN_MAX_AGE_SECONDS = 7 * 24 * 3600       # tokens valid for 7 days
# Regular expression for strict email validation (RFC‐inspired, not fully RFC‐compliant)
EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$")

# Initialize Flask
app = Flask(__name__)
app.config["PREFERRED_URL_SCHEME"] = "https"    # secure default
app.secret_key = SECRET_KEY                    # for securely signing tokens

# Serializer for unsubscribe tokens
serializer = URLSafeTimedSerializer(SECRET_KEY, salt="unsubscribe-token-salt")

# -----------------------------------------------------------------------------
# Utility functions
# -----------------------------------------------------------------------------

def get_db_connection():
    """
    Returns a new MySQLdb connection with least-privileged user.
    Caller must close the connection.
    """
    try:
        conn = MySQLdb.connect(
            host=os.environ["DB_HOST"],
            user=os.environ["DB_USER"],
            passwd=os.environ["DB_PASSWORD"],
            db=os.environ["DB_NAME"],
            charset="utf8mb4",
            use_unicode=True,
            connect_timeout=5
        )
        return conn
    except MySQLdb.Error as e:
        # Do not leak DB details
        logger.error("Database connection failed")
        raise

def validate_email(email):
    """
    Strictly validate email format to prevent SQL injection and other attacks.
    """
    if not email or len(email) > 254:
        return False
    return EMAIL_REGEX.fullmatch(email) is not None

def validate_token(token):
    """
    Validate the unsubscribe token and return the signed data if valid.
    """
    try:
        data = serializer.loads(token, max_age=TOKEN_MAX_AGE_SECONDS)
        # Expect data to be a dict containing an 'email' key
        if not isinstance(data, dict) or "email" not in data:
            raise BadSignature("Invalid token payload")
        return data
    except SignatureExpired:
        logger.info("Unsubscribe token expired")
        abort(400, description="Token expired or invalid")
    except BadSignature:
        logger.info("Unsubscribe token signature invalid")
        abort(400, description="Token expired or invalid")

# -----------------------------------------------------------------------------
# Error handlers
# -----------------------------------------------------------------------------

def generic_error(message="An error occurred"):
    """
    Return a generic error response without leaking details.
    """
    resp = make_response(message, 400)
    resp.headers["Content-Type"] = "text/plain; charset=utf-8"
    return resp

@app.errorhandler(400)
def bad_request(e):
    return generic_error("Bad request")

@app.errorhandler(403)
def forbidden(e):
    return generic_error("Forbidden")

@app.errorhandler(404)
def not_found(e):
    return generic_error("Not found")

@app.errorhandler(500)
def internal_error(e):
    logger.exception("Internal server error")
    return generic_error("Internal server error")

# -----------------------------------------------------------------------------
# Unsubscribe endpoint
# -----------------------------------------------------------------------------

@app.route("/unsubscribe", methods=["GET"])
def unsubscribe():
    """
    Securely handle unsubscribe requests.
    Requires two GET parameters:
     - email: user email address
     - token: signed token protecting against CSRF and forging
    """
    # Extract inputs
    email = request.args.get("email", "").strip()
    token = request.args.get("token", "").strip()

    # Validate inputs early
    if not validate_email(email):
        logger.info("Invalid email format provided to unsubscribe")
        abort(400)
    if not token or len(token) > 512:
        logger.info("Invalid token format or length")
        abort(400)

    # Validate token authenticity and expiration
    data = validate_token(token)
    token_email = data.get("email")
    # Ensure token email matches provided email
    if token_email.lower() != email.lower():
        logger.info("Token email does not match query email")
        abort(400)

    # Perform unsubscribe operation with least privilege
    try:
        conn = get_db_connection()
        try:
            cursor = conn.cursor()
            # Use parameterized queries to prevent SQL injection
            update_stmt = (
                "UPDATE newsletter_subscribers "
                "SET unsubscribed = %s, unsubscribed_at = UTC_TIMESTAMP() "
                "WHERE email = %s AND unsubscribed = %s"
            )
            params = (1, email, 0)
            cursor.execute(update_stmt, params)
            conn.commit()
            # We do not disclose whether the email existed to prevent enumeration
        finally:
            cursor.close()
            conn.close()
    except Exception:
        # All database errors result in a generic response
        abort(500)

    # Return generic success message
    resp = make_response(
        "If your email was subscribed, it has now been unsubscribed.",
        200
    )
    resp.headers["Content-Type"] = "text/plain; charset=utf-8"
    return resp

# -----------------------------------------------------------------------------
# Token generation utility (for internal use)
# -----------------------------------------------------------------------------
def generate_unsubscribe_token(email):
    """
    Generate a time-limited, signed unsubscribe token for a given email.
    This function should be used when sending the unsubscribe link in emails.
    """
    # Validate email before token generation
    if not validate_email(email):
        raise ValueError("Invalid email")
    payload = {"email": email}
    token = serializer.dumps(payload)