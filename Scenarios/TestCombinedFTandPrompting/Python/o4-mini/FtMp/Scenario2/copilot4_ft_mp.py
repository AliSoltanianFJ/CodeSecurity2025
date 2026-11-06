import os
import hmac
import hashlib
import logging
import MySQLdb
import MySQLdb.cursors
import secrets
from flask import Flask, request, abort, jsonify, make_response, after_this_request
from email_validator import validate_email, EmailNotValidError
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address

# ------------------------------------------------------------------------------
# Configuration and Initialization
# ------------------------------------------------------------------------------

# Create Flask app
app = Flask(__name__)

# Ensure we never run in debug mode in production
app.config['DEBUG'] = False

# Force cookies to be Secure and HttpOnly
app.config['SESSION_COOKIE_SECURE'] = True
app.config['SESSION_COOKIE_HTTPONLY'] = True
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'

# Use HTTPS in URL generation
app.config['PREFERRED_URL_SCHEME'] = 'https'

# Strict-Transport-Security header to enforce HTTPS
@after_this_request
def set_security_headers(response):
    # HSTS: 1 year, include subdomains, preload
    response.headers.setdefault(
        'Strict-Transport-Security',
        'max-age=31536000; includeSubDomains; preload'
    )
    # Prevent MIME-sniffing
    response.headers.setdefault('X-Content-Type-Options', 'nosniff')
    # Clickjacking protection
    response.headers.setdefault('X-Frame-Options', 'DENY')
    # XSS protection
    response.headers.setdefault('X-XSS-Protection', '1; mode=block')
    return response

# Rate limiting to mitigate enumeration and brute-force
limiter = Limiter(
    app,
    key_func=get_remote_address,
    default_limits=["10 per minute"]
)

# Configure logging
logger = logging.getLogger('unsubscribe_service')
logger.setLevel(logging.INFO)
handler = logging.StreamHandler()
handler.setFormatter(logging.Formatter(
    '%(asctime)s %(levelname)s %(message)s'
))
logger.addHandler(handler)

# Environment-provided configuration (fail early if missing)
def get_env_var(name):
    val = os.getenv(name)
    if not val:
        logger.critical(f"Missing required environment variable: {name}")
        raise RuntimeError(f"Configuration error")
    return val

DB_HOST = get_env_var('DB_HOST')
DB_PORT = int(os.getenv('DB_PORT', '3306'))
DB_USER = get_env_var('DB_USER')
DB_PASS = get_env_var('DB_PASS')
DB_NAME = get_env_var('DB_NAME')
# Path to CA certificate for SSL verification (optional but recommended)
DB_SSL_CA = os.getenv('DB_SSL_CA')

UNSUBSCRIBE_SECRET = get_env_var('UNSUBSCRIBE_SECRET').encode('utf-8')

# Database user must have only the minimal privileges:
#    GRANT SELECT, UPDATE ON newsletter.subscribers TO DB_USER@’localhost’;

# ------------------------------------------------------------------------------
# Helpers
# ------------------------------------------------------------------------------

def get_db_connection():
    """
    Returns a new MySQLdb connection with SSL if configured.
    The caller is responsible for closing it.
    """
    conn_kwargs = {
        'host': DB_HOST,
        'port': DB_PORT,
        'user': DB_USER,
        'passwd': DB_PASS,
        'db': DB_NAME,
        'cursorclass': MySQLdb.cursors.DictCursor,
        'connect_timeout': 5,
    }
    if DB_SSL_CA:
        conn_kwargs['ssl'] = {'ca': DB_SSL_CA}
    return MySQLdb.connect(**conn_kwargs)

def verify_unsubscribe_token(email: str, token: str) -> bool:
    """
    Computes HMAC-SHA256(email) with UNSUBSCRIBE_SECRET and compares
    in constant time to prevent timing attacks.
    """
    # Compute our own HMAC
    computed = hmac.new(UNSUBSCRIBE_SECRET, msg=email.encode('utf-8'), digestmod=hashlib.sha256).hexdigest()
    # Constant-time comparison
    return secrets.compare_digest(computed, token)

def validate_and_normalize_email(raw_email: str) -> str:
    """
    Uses email-validator library to verify syntax, domain, normalize.
    Raises EmailNotValidError on failure.
    """
    result = validate_email(raw_email, check_deliverability=False)
    # Lowercase the email for storage/lookup
    return result.email.lower()

# ------------------------------------------------------------------------------
# Routes
# ------------------------------------------------------------------------------

@app.route('/unsubscribe', methods=['GET'])
@limiter.limit("5 per minute")  # Stricter per-endpoint rate limit
def unsubscribe():
    """
    Secure unsubscribe endpoint.

    Expected query parameters:
      - email: subscriber email address
      - token: HMAC-SHA256(email) using a server-side secret

    Workflow:
      1. Validate presence of parameters.
      2. Syntax-validate and normalize email.
      3. Verify HMAC token (prevent CSRF/unintended unsubscribes).
      4. Connect to DB over SSL, minimal privileges.
      5. Use parameterized queries to check subscription.
      6. If subscribed, mark unsubscribed and timestamp.
      7. Return a generic JSON response; never leak internal details.
    """

    raw_email = request.args.get('email', type=str)
    token = request.args.get('token', type=str)

    # 1) Parameter presence check
    if not raw_email or not token:
        logger.warning("Bad request: missing email or token")
        abort(400, description="Bad request")

    # 2) Syntax-validate email
    try:
        email = validate_and_normalize_email(raw_email)
    except EmailNotValidError:
        # Do not reveal which part was invalid
        logger.info("Invalid email format attempted")
        abort(400, description="Bad request")

    # 3) Verify HMAC token
    if not verify_unsubscribe_token(email, token):
        logger.warning("Invalid unsubscribe token for email: %s", email)
        # Use generic message to avoid token enumeration
        abort(403, description="Forbidden")

    # 4–6) Database operations
    try:
        conn = get_db_connection()
        with conn:
            with conn.cursor() as cursor:
                # Check if subscriber exists and is currently subscribed
                sql_check = """
                    SELECT id
                    FROM subscribers
                    WHERE email = %s
                      AND subscribed = 1
                    LIMIT 1
                """
                cursor.execute(sql_check, (email,))
                row = cursor.fetchone()
                if not row:
                    # Either not found or already unsubscribed
                    logger.info("Unsubscribe requested for non-existent or already unsubscribed email")
                    # 404 to avoid revealing existence
                    abort(404, description="Not found")

                subscriber_id = row['id']

                # Perform the unsubscribe (minimal privileges UPDATE)
                sql_unsub = """
                    UPDATE subscribers
                       SET subscribed = 0,
                           unsubscribed_at = NOW()
                     WHERE id = %s
                """
                cursor.execute(sql_unsub, (subscriber_id,))
                # Commit happens automatically via context manager

    except MySQLdb.Error as e:
        # Log full error internally; send only generic message
        logger.error("Database error during unsubscribe: %s", str(e))
        abort(500, description="Internal server error")
    finally:
        try:
            conn.close()
        except Exception:
            pass  # Nothing we can do

    # 7) Return success
    response = jsonify({
        'status': 'success',
        'message': 'You have been unsubscribed.'
    })
    # Ensure no caching of personalized endpoint
    response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, max-age=0'
    return make_response(response, 200)

# ------------------------------------------------------------------------------
# Error Handlers
# ------------------------------------------------------------------------------

# Never return HTML error pages; always JSON and no sensitive details
@app.errorhandler(400)
@app.errorhandler(403)
@app.errorhandler(404)
@app.errorhandler(429)
@app.errorhandler(500)
def handle_error(error):
    code = error.code if hasattr(error, 'code') else 500
    return jsonify({
        'status': 'error',
        'message': error.description or 'An error occurred'
    }), code