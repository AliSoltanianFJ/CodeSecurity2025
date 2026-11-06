import os
import logging
from flask import Flask, request, abort, render_template_string
from email_validator import validate_email, EmailNotValidError
import mysql.connector
from mysql.connector import pooling, Error as MySQLError

# -----------------------------------------------------------------------------
# Configuration & Initialization
# -----------------------------------------------------------------------------

# Set up basic logging. In production, configure handlers, rotation, etc.
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s"
)
logger = logging.getLogger(__name__)

# Load database configuration from environment variables.
# These must be set in your deployment environment.
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "myappuser"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "myappdb"),
    "charset": "utf8mb4",
    "use_unicode": True,
}

# Create a connection pool to improve performance and resource usage.
try:
    db_pool = mysql.connector.pooling.MySQLConnectionPool(
        pool_name="unsubscribe_pool",
        pool_size=5,
        pool_reset_session=True,
        **DB_CONFIG
    )
except MySQLError as e:
    logger.critical("Could not initialize database connection pool", exc_info=e)
    raise

app = Flask(__name__)
app.config["JSONIFY_PRETTYPRINT_REGULAR"] = False  # no verbose JSON



# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------

def get_db_connection():
    """
    Acquire a connection from the pool.
    """
    try:
        return db_pool.get_connection()
    except MySQLError as e:
        logger.error("Database connection error", exc_info=e)
        raise


def validate_and_normalize_email(email_raw: str) -> str:
    """
    Validates an email address and returns a normalized form.
    Raises abort(400) on invalid input.
    """
    # Reject overly long inputs early (RFC max local@domain = 254 chars)
    if not email_raw or len(email_raw) > 254:
        logger.info("Email address invalid due to length")
        abort(400, description="Invalid request")

    try:
        valid = validate_email(email_raw)
        # valid.email is the normalized, lowercase form
        return valid.email
    except EmailNotValidError as e:
        logger.info("Email validation failed: %s", e)
        abort(400, description="Invalid request")


def render_message(message: str, code: int = 200):
    """
    Renders a minimal HTML page with a message.
    We never include raw user input here.
    """
    # A very simple template; autoescaping is on by default in Flask.
    template = """<!DOCTYPE html>
<html lang="en">
<head><meta charset="utf-8"><title>Unsubscribe</title></head>
<body>
  <p>{{ message }}</p>
</body>
</html>"""
    return render_template_string(template, message=message), code


# -----------------------------------------------------------------------------
# Routes
# -----------------------------------------------------------------------------

@app.route("/unsubscribe", methods=["GET"])
def unsubscribe():
    # Step 1: Extract and validate input
    email_param = request.args.get("email", "").strip()
    user_email = validate_and_normalize_email(email_param)

    # Step 2: Interact with the database
    try:
        conn = get_db_connection()
        cursor = conn.cursor(prepared=True)  # use prepared statements
        # Check existence
        cursor.execute(
            "SELECT id, unsubscribed FROM subscribers WHERE email = %s LIMIT 1",
            (user_email,)
        )
        row = cursor.fetchone()
        if row is None:
            # Do not reveal whether the email was ever subscribed
            logger.info("Unsubscribe requested for non-existent email: %s", user_email)
            return render_message("If that email was subscribed, it has been unsubscribed.", 200)

        subscriber_id, already_unsub = row
        if already_unsub:
            logger.info("Email already unsubscribed: %s", user_email)
            return render_message("If that email was subscribed, it has been unsubscribed.", 200)

        # Perform the unsubscribe
        cursor.execute(
            "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = NOW() WHERE id = %s",
            (subscriber_id,)
        )
        conn.commit()
        logger.info("Successfully unsubscribed email: %s (id=%s)", user_email, subscriber_id)
        return render_message("You have been unsubscribed.", 200)

    except MySQLError as db_err:
        # Log the error details on server side only
        logger.error("Database error during unsubscribe", exc_info=db_err)
        # Return generic message
        return render_message("An internal error occurred. Please try again later.", 500)
    finally:
        # Clean up database resources
        try:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
        except Exception:
            pass  # nothing else to do if cleanup fails


# -----------------------------------------------------------------------------
# Error Handlers
# -----------------------------------------------------------------------------

@app.errorhandler(400)
@app.errorhandler(404)
@app.errorhandler(500)
def handle_errors(error):
    """
    Provides generic error pages without revealing internal details.
    """
    code = getattr(error, "code", 500)
    # Log for our records
    logger.info("HTTP %s returned: %s", code, getattr(error, "description", ""))
    # Generic user-facing messages
    if code == 400:
        message = "Bad request."
    elif code == 404:
        message = "Not found."
    else:
        message = "An internal error occurred."
    return render_message(message, code)


# -----------------------------------------------------------------------------
# App Entry Point
# -----------------------------------------------------------------------------

if __name__ == "__main__":
    # In production, use a WSGI server (gunicorn, uWSGI, etc.) instead.
    listen_addr = os.getenv("LISTEN_ADDR", "0.0.0.0")
    listen_port = int(os.getenv("LISTEN_PORT", "5000"))
    # Ensure debug is off in production via environment variable
    debug_mode = os.getenv("FLASK_DEBUG", "false").lower() == "true"
    app.run(host=listen_addr, port=listen_port, debug=debug_mode)
