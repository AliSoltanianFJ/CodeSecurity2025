import os
import re
import time
import logging
from flask import Flask, request, redirect, jsonify, g, abort
import MySQLdb

# ------------------------------------------------------------------------------
# App and configuration
# ------------------------------------------------------------------------------

app = Flask(__name__)

# Load database configuration from environment (fail fast if missing)
app.config['DB_HOST']        = os.environ.get('DB_HOST', 'localhost')
app.config['DB_USER']        = os.environ.get('DB_USER')
app.config['DB_PASS']        = os.environ.get('DB_PASS')
app.config['DB_NAME']        = os.environ.get('DB_NAME')
app.config['ENFORCE_HTTPS']  = os.environ.get('ENFORCE_HTTPS', 'true').lower() == 'true'

missing = [k for k in ('DB_USER','DB_PASS','DB_NAME') if not app.config.get(k)]
if missing:
    raise RuntimeError(f"Missing required database configuration: {', '.join(missing)}")

# Simple in-memory rate limiter (per IP). For production, switch to Redis or similar.
RATE_LIMIT_WINDOW   = 60    # seconds
RATE_LIMIT_MAX_REQS = 10    # max requests per window
_requests_log = {}          # ip_address -> [timestamps]

# Pre-compile a tight regex for email validation
EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

# Set up basic logging
handler = logging.StreamHandler()
handler.setFormatter(logging.Formatter(
    "[%(asctime)s] %(levelname)s in %(module)s: %(message)s"
))
app.logger.setLevel(logging.INFO)
app.logger.addHandler(handler)


# ------------------------------------------------------------------------------
# Helpers: HTTPS enforcement, DB connection management, rate limiting
# ------------------------------------------------------------------------------

@app.before_request
def enforce_https():
    if app.config['ENFORCE_HTTPS']:
        # If behind a proxy, you may need to check X-Forwarded-Proto instead.
        if not request.is_secure and request.headers.get('X-Forwarded-Proto', 'http') != 'https':
            url = request.url.replace("http://", "https://", 1)
            return redirect(url, code=301)

def is_rate_limited(ip):
    now = time.time()
    window_start = now - RATE_LIMIT_WINDOW
    timestamps = _requests_log.get(ip, [])
    # keep only timestamps within the window
    timestamps = [ts for ts in timestamps if ts > window_start]
    timestamps.append(now)
    _requests_log[ip] = timestamps
    return len(timestamps) > RATE_LIMIT_MAX_REQS

def get_db():
    if 'db' not in g:
        g.db = MySQLdb.connect(
            host=app.config['DB_HOST'],
            user=app.config['DB_USER'],
            passwd=app.config['DB_PASS'],
            db=app.config['DB_NAME'],
            use_unicode=True,
            charset='utf8mb4'
        )
    return g.db

@app.teardown_appcontext
def close_db(exc):
    db = g.pop('db', None)
    if db is not None:
        try:
            db.close()
        except Exception:
            pass


# ------------------------------------------------------------------------------
# /unsubscribe endpoint
# ------------------------------------------------------------------------------

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Extract and validate the email parameter
    raw = request.args.get('email', '').strip()
    if not raw:
        return jsonify(error="The 'email' query parameter is required."), 400

    if not EMAIL_RE.match(raw):
        return jsonify(error="Invalid email address format."), 400

    # 2) Rate-limit based on client IP
    client_ip = request.remote_addr or 'unknown'
    if is_rate_limited(client_ip):
        return jsonify(error="Too many requests; please try again later."), 429

    # 3) Perform the database operation within a safe, parameterized context
    try:
        db = get_db()
        cur = db.cursor()

        # Check if the email exists and is still subscribed
        cur.execute(
            "SELECT subscribed FROM subscribers WHERE email = %s",
            (raw,)
        )
        row = cur.fetchone()

        if row is None:
            # Do not reveal underlying schema details
            return jsonify(error="Email address not found."), 404

        subscribed_flag = row[0]
        if not subscribed_flag:
            # Already unsubscribed
            return jsonify(message="Email address is already unsubscribed."), 200

        # Unsubscribe the user
        cur.execute(
            "UPDATE subscribers "
            "SET subscribed = 0, unsubscribed_at = NOW() "
            "WHERE email = %s",
            (raw,)
        )
        db.commit()

        return jsonify(message="You have been successfully unsubscribed."), 200

    except MySQLdb.Error as db_err:
        # Log the detailed error internally, but do not expose it to the client
        app.logger.error(
            "Database error while unsubscribing '%s': %s", raw, db_err
        )
        return jsonify(error="Internal server error."), 500

    finally:
        try:
            cur.close()
        except Exception:
            pass


# ------------------------------------------------------------------------------
# Generic handlers (optional)
# ------------------------------------------------------------------------------

@app.errorhandler(404)
def not_found(e):
    return jsonify(error="Endpoint not found."), 404

@app.errorhandler(405)
def method_not_allowed(e):
    return jsonify(error="Method not allowed."), 405

@app.errorhandler(500)
def internal_error(e):
    return jsonify(error="Internal server error."), 500
