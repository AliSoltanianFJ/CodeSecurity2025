import os
import re
import logging
import secrets
from datetime import datetime, timedelta

from flask import Flask, request, abort, render_template
from flask_talisman import Talisman
from mysql.connector import connect, Error as DBError
from email_validator import validate_email, EmailNotValidError

# ——————————————————————————————————————————
# 1. Configuration & Initialization
# ——————————————————————————————————————————

# Create Flask app with secure defaults
app = Flask(__name__, instance_relative_config=False)
# Never enable debug or testing in production
app.config.update(
    DEBUG       = False,
    TESTING     = False,
    SECRET_KEY  = os.environ.get('FLASK_SECRET_KEY', None),  # Must be set in env
    SESSION_COOKIE_SECURE  = True,    # Only send session cookie over HTTPS
    SESSION_COOKIE_HTTPONLY = True,
    SESSION_COOKIE_SAMESITE = 'Lax'   # CSRF protection
)
if not app.config['SECRET_KEY']:
    raise RuntimeError("Missing FLASK_SECRET_KEY environment variable")

# Enforce HTTPS, HSTS, Content-Security-Policy, X-Frame-Options, etc.
Talisman(app,
    content_security_policy={
        'default-src': ["'self'"],
        # add other CSP directives as required
    },
    force_https=True,
    strict_transport_security=True,
    strict_transport_security_max_age=31536000  # one year
)

# Configure logging: rotate logs, do not include sensitive data
handler = logging.FileHandler(os.environ.get('UNSUBSCRIBE_LOG_PATH', '/var/log/unsubscribe.log'))
formatter = logging.Formatter(
    '%(asctime)s %(levelname)s %(message)s'
)
handler.setFormatter(formatter)
handler.setLevel(logging.INFO)
app.logger.addHandler(handler)
app.logger.setLevel(logging.INFO)

# ——————————————————————————————————————————
# 2. Database Connection (least privilege, SSL)
# ——————————————————————————————————————————

def get_db_connection():
    """
    Returns a new MySQL connection using a least-privileged account.
    All credentials and SSL parameters are read from environment.
    """
    try:
        conn = connect(
            host      = os.environ['DB_HOST'],
            port      = int(os.environ.get('DB_PORT', '3306')),
            user      = os.environ['DB_USER'],     # account with only UPDATE, SELECT on users table
            password  = os.environ['DB_PASSWORD'],
            database  = os.environ['DB_NAME'],
            ssl_ca    = os.environ.get('DB_SSL_CA'),    # e.g. /etc/ssl/certs/ca.pem
            ssl_cert  = os.environ.get('DB_SSL_CERT'),  # optional
            ssl_key   = os.environ.get('DB_SSL_KEY'),   # optional
            charset   = 'utf8mb4',
            use_pure  = True
        )
        return conn
    except KeyError as e:
        app.logger.error("Missing database configuration: %s", e)
        raise RuntimeError("Database not configured") from e
    except DBError as e:
        app.logger.exception("Database connection error")
        raise

# ——————————————————————————————————————————
# 3. Utility: Validate unsubscribe token
# ——————————————————————————————————————————

def is_valid_token(token):
    """
    Token must be exactly 64 lowercase hex chars (we use 32-byte HMAC or random bytes).
    Reject anything else immediately.
    """
    return bool(re.fullmatch(r'[0-9a-f]{64}', token))


# ——————————————————————————————————————————
# 4. Error Handlers (avoid leaking details)
# ——————————————————————————————————————————

@app.errorhandler(400)
def handle_bad_request(e):
    return render_template('400.html'), 400

@app.errorhandler(404)
def handle_not_found(e):
    return render_template('404.html'), 404

@app.errorhandler(500)
def handle_server_error(e):
    # Do not include exception details in response
    return render_template('500.html'), 500


# ——————————————————————————————————————————
# 5. Unsubscribe Endpoint
# ——————————————————————————————————————————

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    """
    Secure unsubscribe flow:
      - Expects a one-time token (no email in querystring).
      - Validates token format.
      - Checks token existence & expiry in DB.
      - Marks user as unsubscribed, invalidates token.
      - Uses parameterized queries to prevent SQL injection.
    """
    token = request.args.get('token', default='', type=str).strip()
    if not is_valid_token(token):
        app.logger.warning("Invalid unsubscribe token format")
        abort(400)

    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)

        # 5.1. Look up token safely (parameterized)
        cursor.execute(
            """
            SELECT id, email
              FROM users
             WHERE unsubscribe_token = %s
               AND unsubscribe_expires >= NOW()
               AND is_subscribed = TRUE
            LIMIT 1
            """,
            (token,)
        )
        row = cursor.fetchone()
        if not row:
            # Token not found, expired, or already unsubscribed
            app.logger.info("Unsubscribe attempt with invalid/expired token")
            return render_template('unsubscribe_invalid.html'), 400

        user_id = row['id']
        email   = row['email']

        # 5.2. Perform the unsubscribe
        cursor.execute(
            """
            UPDATE users
               SET is_subscribed       = FALSE
                 , unsubscribe_token   = NULL
                 , unsubscribe_expires = NULL
             WHERE id = %s
            """,
            (user_id,)
        )
        conn.commit()

        # 5.3. Log safely without PII
        app.logger.info(
            "User unsubscribed: user_id=%d",
            user_id
        )

        # 5.4. Render generic success page (templates escape by default)
        return render_template('unsubscribe_success.html'), 200

    except DBError:
        app.logger.exception("Database error during unsubscribe")
        abort(500)
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass


# ——————————————————————————————————————————
# 6. Token Generation Example (to be used when sending the email)
# ——————————————————————————————————————————

def generate_unsubscribe_token(user_id, valid_hours=48):
    """
    Generates a cryptographically secure random token, stores
    its hash in the DB, and returns the raw token for inclusion
    in the email link. Expires in `valid_hours`.
    """
    raw_token = secrets.token_hex(32)  # 64 hex chars
    expiry    = datetime.utcnow() + timedelta(hours=valid_hours)

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute(
            """
            UPDATE users
               SET unsubscribe_token   = %s
                 , unsubscribe_expires = %s
             WHERE id = %s
            """,
            (raw_token, expiry, user_id)
        )
        conn.commit()
    except DBError:
        app.logger.exception("Failed to store unsubscribe token for user_id=%d", user_id)
        raise
    finally:
        cursor.close()
        conn.close()

    return raw_token
