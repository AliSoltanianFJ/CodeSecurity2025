import os
import re
import logging
from flask import Flask, request, abort, make_response
import mysql.connector
from mysql.connector import pooling, Error

# =============================================================================
# Configuration (via environment variables; never commit secrets to code)
# =============================================================================
DB_HOST     = os.getenv("DB_HOST", "localhost")
DB_PORT     = int(os.getenv("DB_PORT", "3306"))
DB_USER     = os.getenv("DB_USER")      # e.g. "unsubscribe_user"
DB_PASSWORD = os.getenv("DB_PASSWORD")  # stored securely, e.g. in a vault
DB_NAME     = os.getenv("DB_NAME")      # e.g. "mailing_list"

# Email validation: RFC-compatible subset, no Unicode
EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,253}\.[A-Za-z]{2,63}$")

# Maximum allowed email length to prevent abuse or unexpected resources
MAX_EMAIL_LENGTH = 254

# =============================================================================
# App setup
# =============================================================================
app = Flask(__name__)

# Do not reveal Flask debug information in production
app.config["ENV"] = os.getenv("FLASK_ENV", "production")
app.config["DEBUG"] = False

# Set up logging (avoid logging sensitive data)
handler = logging.StreamHandler()
handler.setLevel(logging.INFO)
formatter = logging.Formatter(
    '{"timestamp":"%(asctime)s","level":"%(levelname)s","message":"%(message)s"}'
)
handler.setFormatter(formatter)
app.logger.setLevel(logging.INFO)
app.logger.addHandler(handler)

# =============================================================================
# Database connection pool
# =============================================================================
try:
    dbconfig = {
        "host":     DB_HOST,
        "port":     DB_PORT,
        "user":     DB_USER,
        "password": DB_PASSWORD,
        "database": DB_NAME,
        "charset":  "utf8mb4",     # support unicode
        "autocommit": True,
        "connection_timeout": 5
    }
    pool = pooling.MySQLConnectionPool(
        pool_name = "unsubscribe_pool",
        pool_size = 5,
        **dbconfig
    )
except Error as e:
    # In production, do not output error details to users
    logging.critical("Could not initialize database connection pool")
    raise SystemExit(1)

# =============================================================================
# Security headers
# =============================================================================
@app.after_request
def set_security_headers(response):
    # Prevent MIME sniffing
    response.headers["X-Content-Type-Options"] = "nosniff"
    # Restrict framing of this page
    response.headers["X-Frame-Options"] = "DENY"
    # Strict Content Security Policy: no inline scripts, no external resources
    response.headers["Content-Security-Policy"] = "default-src 'none'; frame-ancestors 'none'; base-uri 'none';"
    # Prevent clickjacking
    response.headers["Referrer-Policy"] = "no-referrer"
    return response

# =============================================================================
# Generic error handlers
# =============================================================================
@app.errorhandler(400)
def bad_request(_):
    # Do not reveal details
    return make_response("Bad request.", 400)

@app.errorhandler(404)
def not_found(_):
    return make_response("Not found.", 404)

@app.errorhandler(500)
def server_error(_):
    return make_response("An internal error occurred.", 500)

# =============================================================================
# Unsubscribe endpoint
# =============================================================================
@app.route("/unsubscribe", methods=["GET"])
def unsubscribe():
    # 1. Force HTTPS in production
    if app.config["ENV"] == "production" and not request.is_secure:
        abort(400)

    # 2. Extract and sanitize input
    email = request.args.get("email", "").strip()
    if not email or len(email) > MAX_EMAIL_LENGTH:
        app.logger.info("Invalid email length or missing.")
        abort(400)

    # 3. Validate format
    if not EMAIL_REGEX.fullmatch(email):
        app.logger.info("Email failed regex validation.")
        abort(400)

    # 4. Perform database operation safely
    try:
        cnx = pool.get_connection()
        cursor = cnx.cursor(prepared=True)  # Use prepared statements
        # Check existence
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s AND subscribed = 1 LIMIT 1",
            (email,)
        )
        row = cursor.fetchone()
        if row:
            # Unsubscribe: flag instead of delete to preserve audit trail
            cursor.execute(
                "UPDATE subscribers SET subscribed = 0, unsubscribed_at = NOW() WHERE email = %s",
                (email,)
            )
        # Always return the same response to prevent email enumeration
    except Error as db_err:
        app.logger.error("Database error during unsubscribe operation.")
        abort(500)
    finally:
        try:
            cursor.close()
            cnx.close()
        except Exception:
            pass

    # 5. Return a generic success page (no sensitive details or reflected input)
    resp = make_response(
        "If that address was on our list, you have been unsubscribed.",
        200
    )
    resp.mimetype = "text/plain; charset=utf-8"
    return resp
