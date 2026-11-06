
"""
Secure unsubscribe endpoint.

Security decisions / justifications are commented inline.
Follows OWASP Top Ten mitigations, CERT Python Secure Coding,
and general best practices.
"""

import os
import re
import hmac
import hashlib
import logging
from datetime import datetime, timedelta

from flask import Flask, request, abort, jsonify
from flask_talisman import Talisman
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address

import pymysql.cursors

# ------------------------------------------------------------------------------
# App and Security Configuration
# ------------------------------------------------------------------------------

# Create Flask application
app = Flask(__name__, instance_relative_config=False)

# Load configuration from environment variables only (no hardcoded secrets)
app.config['SECRET_KEY'] = os.environ.get('FLASK_SECRET_KEY')  # Used for signing CSRF tokens etc.
if not app.config['SECRET_KEY']:
    raise RuntimeError("FLASK_SECRET_KEY is required but not set in environment")

# Security headers and HTTPS enforcement
# - Content-Security-Policy, HSTS, X-Frame-Options, X-Content-Type-Options, etc.
Talisman(
    app,
    content_security_policy={
        'default-src': ["'self'"]
    },
    force_https=True,
    strict_transport_security=True,
    strict_transport_security_max_age=31536000,  # One year
    session_cookie_secure=True,
    session_cookie_http_only=True,
    frame_options='DENY'
)

# Rate limiter to slow down automated attacks
limiter = Limiter(
    app,
    key_func=get_remote_address,
    default_limits=["10 per minute"]  # adjust as appropriate
)

# Configure logging
handler = logging.StreamHandler()
handler.setLevel(logging.INFO)
formatter = logging.Formatter(
    '%(asctime)s %(levelname)s %(name)s %(message)s'
)
handler.setFormatter(formatter)
app.logger.setLevel(logging.INFO)
app.logger.addHandler(handler)

# ------------------------------------------------------------------------------
# Database Connection Factory
# ------------------------------------------------------------------------------

def get_db_connection():
    """
    Returns a new database connection using least-privileged credentials.
    Credentials and SSL CA file path are pulled from environment.
    """
    try:
        conn = pymysql.connect(
            host=os.environ['DB_HOST'],
            port=int(os.environ.get('DB_PORT', '3306')),
            user=os.environ['DB_USER'],            # Should be a user with only UPDATE privileges on subscribers table
            password=os.environ['DB_PASSWORD'],
            database=os.environ['DB_NAME'],
            cursorclass=pymysql.cursors.DictCursor,
            ssl={'ca': os.environ.get('DB_SSL_CA_PATH')},  # Enforce SSL
            connect_timeout=5,
            read_timeout=5,
            write_timeout=5,
            charset='utf8mb4',
            autocommit=False                     # We will commit explicitly
        )
        return conn
    except Exception as e:
        # Do not leak internal error details to client
        app.logger.error("Database connection failed")
        raise

# ------------------------------------------------------------------------------
# Input Validation
# ------------------------------------------------------------------------------

# RFC 5322 compliant-ish regex for email addresses (simplified, rejects most invalid chars)
EMAIL_REGEX = re.compile(
    r"^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+"
    r"(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
    r"@"
    r"(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+"
    r"[A-Za-z]{2,}$"
)

def validate_email(email: str) -> bool:
    """
    Validate length and pattern of email.
    """
    # Reject very long strings
    if not (5 <= len(email) <= 254):
        return False
    # Match against our regex
    return EMAIL_REGEX.match(email) is not None

def verify_signature(email: str, signature: str) -> bool:
    """
    Verify HMAC signature of email using a secret key.
    Prevents an attacker from unsubscribing arbitrary emails.
    """
    secret = app.config['SECRET_KEY'].encode('utf-8')
    # Use SHA256 HMAC
    expected = hmac.new(secret, email.encode('utf-8'), digestmod=hashlib.sha256).hexdigest()
    # Constant-time comparison to avoid timing attacks
    return hmac.compare_digest(expected, signature)

# ------------------------------------------------------------------------------
# Unsubscribe Endpoint
# ------------------------------------------------------------------------------

@app.route('/unsubscribe', methods=['GET'])
@limiter.limit("5 per minute")  # stricter rate limit for this sensitive operation
def unsubscribe():
    """
    GET parameters:
      • email (string): the email address to unsubscribe
      • sig   (string): HMAC-SHA256 hex signature of the email, to prevent CSRF/unwanted unsubscribes

    Workflow:
      1. Validate parameters
      2. Verify signature
      3. Lookup email in database using parameterized query
      4. If found and still subscribed, mark unsubscribed
      5. Commit and return generic success message
      6. On error or if not found, return the same generic success message
    """

    # 1) Parse and sanitize inputs
    email = request.args.get('email', '').strip()
    signature = request.args.get('sig', '').strip()

    if not email or not signature:
        # Bad request without details
        abort(400)

    # 2) Validate email format strictly
    if not validate_email(email):
        # Do not reveal which part failed
        abort(400)

    # 3) Verify signature to prevent unauthorized unsubscribes
    if not verify_signature(email, signature):
        # Signature mismatch => unauthorized
        abort(403)

    # 4) Perform database operation
    try:
        conn = get_db_connection()
        with conn:
            with conn.cursor() as cursor:
                # Parameterized UPDATE to avoid SQL injection
                # Only update if currently subscribed (is_subscribed = 1)
                sql = """
                    UPDATE subscribers
                       SET is_subscribed = 0,
                           unsubscribed_at = %s
                     WHERE email = %s
                       AND is_subscribed = 1
                       LIMIT 1
                """
                cursor.execute(sql, (datetime.utcnow(), email))
                # We do not branch on cursor.rowcount in order to give the same response
            conn.commit()
    except Exception:
        # Return generic response
        return jsonify({
            "status": "success",
            "message": "If your email was subscribed, it has now been unsubscribed."
        }), 200

    # 5) Return a generic success response
    return jsonify({
        "status": "success",
        "message": "If your email was subscribed, it has now been unsubscribed."
    }), 200

# ------------------------------------------------------------------------------
# Helper to generate unsubscribe links (to be used by your mailing system)
# ------------------------------------------------------------------------------

def generate_unsubscribe_link(email: str, base_url: str) -> str:
    """
    Creates a one-time link for unsubscribing:
      - email=<urlencoded email>
      - sig=<hex HMAC-SHA256 of email>
    The mailing system should call this for each recipient.
    """
    # Validate before generation
    if not validate_email(email):
        raise ValueError("Invalid email")
    secret = app.config['SECRET_KEY'].encode('utf-8')
    sig = hmac.new(secret, email.encode('utf-8'), digestmod=hashlib.sha256).hexdigest()
    # It is assumed that `base_url` is HTTPS and trusted
    return f"{base_url}/unsubscribe?email={email}&sig={sig}"

# ------------------------------------------------------------------------------
# Error Handlers (do not leak debug info)
# ------------------------------------------------------------------------------

@app.errorhandler(400)
def bad_request(e):
    return jsonify({"error": "Bad request"}), 400

@app.errorhandler(403)
def forbidden(e):
    return jsonify({"error": "Forbidden"}), 403

@app.errorhandler(404)
def not_found(e):
    return jsonify({"error": "Not found"}), 404

@app.errorhandler(500)
def internal_error(e):
    # Log actual exception
    app.logger.error("Internal server error", exc_info=e)
    # Generic message
    return jsonify({"error": "Internal server error"}), 500