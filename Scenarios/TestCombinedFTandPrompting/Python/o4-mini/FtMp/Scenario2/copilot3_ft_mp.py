import os
import re
import logging
from logging.handlers import RotatingFileHandler
from flask import Flask, request, abort, jsonify, make_response
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
import MySQLdb
from MySQLdb.cursors import DictCursor

#
# === Configuration & Initialization ===
#

app = Flask(__name__)

# 1) Do not run in debug mode in production.
app.config['DEBUG'] = False

# 2) Load all sensitive configuration from environment variables only.
DB_HOST = os.getenv('DB_HOST')
DB_PORT = int(os.getenv('DB_PORT', '3306'))
DB_NAME = os.getenv('DB_NAME')
DB_USER = os.getenv('DB_USER')
DB_PASSWORD = os.getenv('DB_PASSWORD')
UNSUBSCRIBE_RATE = os.getenv('UNSUBSCRIBE_RATE', '5 per hour')

# Fail fast if any required variable is missing.
required_env = {
    'DB_HOST': DB_HOST,
    'DB_NAME': DB_NAME,
    'DB_USER': DB_USER,
    'DB_PASSWORD': DB_PASSWORD,
}
missing = [k for k, v in required_env.items() if not v]
if missing:
    raise RuntimeError(f"Missing required environment variables: {', '.join(missing)}")

# 3) Set up structured, non-PII logging, with log rotation.
handler = RotatingFileHandler(
    filename=os.getenv('LOG_FILE', '/var/log/app/unsubscribe.log'),
    maxBytes=10*1024*1024,
    backupCount=5
)
formatter = logging.Formatter(
    '%(asctime)s %(levelname)s %(remote_addr)s %(message)s'
)
handler.setFormatter(formatter)
handler.setLevel(logging.INFO)
app.logger.addHandler(handler)
app.logger.setLevel(logging.INFO)

# 4) Rate-limit unsubscribes to mitigate abuse (Brute force / DoS).
limiter = Limiter(
    app,
    key_func=get_remote_address,
    default_limits=[UNSUBSCRIBE_RATE]
)

#
# === Security-Hardening Middleware ===
#

@app.before_request
def enforce_https_and_hsts():
    # Reject non-HTTPS requests to protect confidentiality/integrity in transit.
    if not request.is_secure:
        # If behind a proxy, ensure it sets X-Forwarded-Proto properly.
        abort(403)
    # Set HSTS header to enforce HTTPS on client side.
    hsts = 'max-age=63072000; includeSubDomains; preload'
    # Use context-aware header setting
    request.environ.setdefault('response_headers', []).append(('Strict-Transport-Security', hsts))

@app.after_request
def set_security_headers(response):
    # Prevent clickjacking.
    response.headers['X-Frame-Options'] = 'DENY'
    # Prevent MIME sniffing.
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Enable simple XSS protection.
    response.headers['X-XSS-Protection'] = '1; mode=block'
    # Content Security Policy (adjust to your needs).
    csp = "default-src 'none'; frame-ancestors 'none'; base-uri 'none';"
    response.headers['Content-Security-Policy'] = csp
    # Append any before_request headers.
    for k, v in request.environ.get('response_headers', []):
        response.headers.setdefault(k, v)
    return response

#
# === Utility Functions ===
#

# Compile once, per RFC 5322 minimal email validation.
EMAIL_REGEX = re.compile(
    r"^(?=.{1,254}$)([A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+"
    r"(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
    r"@"
    r"(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+"
    r"[A-Za-z]{2,})$"
)

def is_valid_email(email: str) -> bool:
    """
    Validate email syntax strictly and enforce max length.
    """
    if not email or len(email) > 254:
        return False
    return EMAIL_REGEX.fullmatch(email) is not None

def get_db_connection():
    """
    Return a new database connection using least-privilege credentials.
    Raises an exception on failure.
    """
    return MySQLdb.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        charset='utf8mb4',
        cursorclass=DictCursor,
        connect_timeout=5,
        autocommit=True  # Avoid multi-statement risk.
    )

def log_info(message: str):
    """
    Centralized logging without PII leakage.
    """
    print(f"[INFO] {message}")

#
# === Error Handlers ===
#

@app.errorhandler(400)
def bad_request(e):
    return make_response(
        jsonify({'error': 'Bad Request'}), 400
    )

@app.errorhandler(403)
def forbidden(e):
    return make_response(
        jsonify({'error': 'Forbidden'}), 403
    )

@app.errorhandler(404)
def not_found(e):
    return make_response(
        jsonify({'error': 'Not Found'}), 404
    )

@app.errorhandler(429)
def too_many_requests(e):
    return make_response(
        jsonify({'error': 'Too many requests; please try later'}), 429
    )

@app.errorhandler(500)
def internal_error(e):
    # Do not reveal exception details.
    return make_response(
        jsonify({'error': 'Internal Server Error'}), 500
    )

#
# === Unsubscribe Endpoint ===
#

@app.route('/unsubscribe', methods=['GET'])
@limiter.limit(UNSUBSCRIBE_RATE)  # Enforce per-IP rate limit.
def unsubscribe():
    """
    Secure unsubscribe endpoint.
    GET parameter:
      - email: user's email address to unsubscribe.
    """
    # 1) Extract and validate input.
    email = request.args.get('email', '', type=str).strip()
    if not is_valid_email(email):
        # Invalid or malformed email provided.
        log_info("Invalid unsubscribe email format attempted.")
        abort(400)

    # 2) Perform unsubscribe in database securely.
    try:
        conn = get_db_connection()
        with conn.cursor() as cur:
            # Use parameterized query to avoid SQL injection.
            select_sql = "SELECT id FROM subscribers WHERE email = %s LIMIT 1"
            cur.execute(select_sql, (email,))
            row = cur.fetchone()
            if row:
                # Principle of least privilege:
                # This user account only has UPDATE privilege on this table.
                update_sql = "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = UTC_TIMESTAMP() WHERE id = %s"
                cur.execute(update_sql, (row['id'],))
                log_info("Unsubscribe succeeded for subscriber id=%s", row['id'])
            else:
                # Do not reveal whether email existed.
                log_info("Unsubscribe attempted for non-existent email.")
        conn.close()
    except MySQLdb.OperationalError:
        # Database connection or query error.
        log_info("Database operational error during unsubscribe.")
        abort(500)
    except Exception:
        # Catch all unexpected exceptions, avoid leaking details.
        log_info("Unexpected error during unsubscribe.")
        abort(500)

    # 3) Return a generic success message.
    return jsonify({
        'status': 'If that email was subscribed, it has now been unsubscribed.'
    }), 200
