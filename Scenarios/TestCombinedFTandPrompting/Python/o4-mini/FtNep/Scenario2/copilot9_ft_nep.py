from flask import Flask, request, redirect
import os
import logging
import MySQLdb
from functools import wraps

app = Flask(__name__)

#
# 1) Configuration validation at startup: fail early if any required DB var is missing
#
_required_env = ("DB_HOST", "DB_USER", "DB_PASS", "DB_NAME", "FLASK_ENV")
_missing = [v for v in _required_env if v not in os.environ]
if _missing:
    raise RuntimeError(f"Missing required environment variables: {', '.join(_missing)}")

#
# 2) Logging: log to stderr; in production you can route this to your log aggregator
#
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
app.logger = logging.getLogger("unsubscribe_app")

#
# 3) Optional: enforce HTTPS redirects when in production
#
@app.before_request
def enforce_https_in_production():
    # FLASK_ENV is 'production' in production deployments
    if os.environ.get("FLASK_ENV") == "production":
        # request.is_secure may not be set behind certain proxies; adjust as needed
        if not request.is_secure:
            url = request.url.replace("http://", "https://", 1)
            return redirect(url, code=301)

#
# 4) Email validation: prefer the email_validator library, fallback to a simple regex
#
try:
    from email_validator import validate_email, EmailNotValidError
    def _validate_and_normalize_address(addr):
        # Will throw EmailNotValidError if invalid
        info = validate_email(addr)
        # normalize to the canonical form
        return info.email.lower()
except ImportError:
    import re
    _simple_email_re = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")
    def _validate_and_normalize_address(addr):
        addr = addr.strip()
        if not _simple_email_re.match(addr):
            raise ValueError("Invalid email format")
        return addr.lower()

#
# 5) Helper to get a new DB connection
#
def get_db_connection():
    return MySQLdb.connect(
        host   = os.environ["DB_HOST"],
        user   = os.environ["DB_USER"],
        passwd = os.environ["DB_PASS"],
        db     = os.environ["DB_NAME"],
        charset         = "utf8mb4",       # prevent charset tricks
        connect_timeout = 5
    )

#
# 6) A simple decorator to catch and log unexpected errors
#
def swallow_exceptions(f):
    @wraps(f)
    def wrapped(*args, **kwargs):
        try:
            return f(*args, **kwargs)
        except MySQLdb.OperationalError:
            app.logger.exception("Database operational error")
            return "Internal Server Error", 500
        except Exception:
            app.logger.exception("Unhandled exception in unsubscribe endpoint")
            return "Internal Server Error", 500
    return wrapped

#
# 7) The unsubscribe endpoint
#
@app.route("/unsubscribe", methods=["GET"])
@swallow_exceptions
def unsubscribe():
    raw_email = request.args.get("email", "").strip()
    if not raw_email:
        return "Bad Request: 'email' parameter is required", 400

    try:
        email = _validate_and_normalize_address(raw_email)
    except (ValueError, NameError, Exception):
        return "Bad Request: invalid email address", 400

    conn   = None
    cursor = None
    try:
        conn   = get_db_connection()
        cursor = conn.cursor()

        # First check: is there a subscribed record?
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s AND subscribed = 1 LIMIT 1",
            (email,)
        )
        if cursor.fetchone():
            # unsubscribe
            cursor.execute(
                "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
                (email,)
            )
            conn.commit()
            return "You have been unsubscribed successfully.", 200

        # If not currently subscribed, see if the email exists at all
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1",
            (email,)
        )
        if cursor.fetchone():
            # existed but already unsubscribed
            return "You were already unsubscribed.", 200
        else:
            # never existed
            return "Not Found: email address not in our records.", 404

    finally:
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass
