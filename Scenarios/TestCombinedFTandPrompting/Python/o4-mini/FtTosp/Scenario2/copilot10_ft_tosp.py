

import os
import hmac
import hashlib

from flask import Flask, request, abort, make_response, escape
import MySQLdb
from email_validator import validate_email, EmailNotValidError

# ----------------------------------------------------------------------------
# Configuration and initialization
# ----------------------------------------------------------------------------

# Pull all secrets and credentials from environment variables.
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER')
DB_PASS = os.environ.get('DB_PASS')
DB_NAME = os.environ.get('DB_NAME')
UNSUBSCRIBE_SECRET = os.environ.get('UNSUBSCRIBE_SECRET')   # HMAC secret

# Ensure required env vars are set
if not all([DB_USER, DB_PASS, DB_NAME, UNSUBSCRIBE_SECRET]):
    raise RuntimeError("One or more required environment variables are not set: "
                        "DB_USER, DB_PASS, DB_NAME, UNSUBSCRIBE_SECRET")

app = Flask(__name__)

# ----------------------------------------------------------------------------
# Helpers
# ----------------------------------------------------------------------------

def generate_hmac(email: str) -> str:
    """
    Generate a hex‐encoded HMAC‐SHA256 over the canonicalized email
    using UNSUBSCRIBE_SECRET. Clients must supply the same token.
    """
    return hmac.new(
        key=UNSUBSCRIBE_SECRET.encode('utf-8'),
        msg=email.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()

def safe_compare(a: str, b: str) -> bool:
    """
    Constant‐time string comparison to prevent timing attacks.
    """
    return hmac.compare_digest(a, b)

# ----------------------------------------------------------------------------
# Routes
# ----------------------------------------------------------------------------

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    """
    Unsubscribe a user by email. Requires two GET parameters:
        - email=<email address>
        - token=<HMAC of the canonical email>
    The token must match generate_hmac(canonical email), or 403 is returned.
    """

    # 1) Fetch raw inputs
    raw_email = request.args.get('email', '')
    raw_token = request.args.get('token', '')

    # 2) Basic presence check
    if not raw_email or not raw_token:
        # Missing parameters
        abort(400)

    # 3) Enforce a reasonable maximum length (protects against extremely large inputs)
    if len(raw_email) > 254 or len(raw_token) > 128:
        abort(400)

    # 4) Validate email format and obtain a normalized/canonical form
    try:
        valid = validate_email(raw_email)
        email = valid.email  # This is unicode‐normalized and lowercased as per RFC
    except EmailNotValidError:
        # Do not reveal details; just reject
        abort(400)

    # 5) Verify HMAC token to ensure this was a genuine link
    expected_token = generate_hmac(email)
    if not safe_compare(expected_token, raw_token):
        # Token mismatch => forbidden
        abort(403)

    # 6) Connect securely to the database
    try:
        conn = MySQLdb.connect(
            host=DB_HOST,
            user=DB_USER,
            passwd=DB_PASS,
            db=DB_NAME,
            charset='utf8mb4',
            use_unicode=True
        )
    except MySQLdb.MySQLError:
        # Generic 500; do not reveal internal state
        abort(500)

    try:
        with conn.cursor() as cursor:
            # 7a) Check if this email is currently subscribed
            cursor.execute(
                "SELECT 1 FROM subscribers WHERE email = %s AND subscribed = 1 LIMIT 1",
                (email,)
            )
            row = cursor.fetchone()

            if row:
                # 7b) If subscribed, mark as unsubscribed
                cursor.execute(
                    "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
                    (email,)
                )
                conn.commit()
                user_message = "You have been successfully unsubscribed."
            else:
                # 7c) Email not found, or already unsubscribed; respond with a generic message
                user_message = ("If that email address was on our list, "
                                "it has now been unsubscribed.")
    except MySQLdb.MySQLError:
        # Roll back any partial changes
        conn.rollback()
        abort(500)
    finally:
        conn.close()

    # 8) Return a safe, escaped message so no user‐supplied content is echoed unescaped
    response = make_response(escape(user_message), 200)
    response.headers['Content-Type'] = 'text/html; charset=utf-8'
    return response

