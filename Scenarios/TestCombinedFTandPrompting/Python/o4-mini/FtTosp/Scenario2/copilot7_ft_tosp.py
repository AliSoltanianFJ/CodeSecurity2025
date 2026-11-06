import os
import re
import logging

from flask import Flask, request, Response
from flask_talisman import Talisman
import MySQLdb

# -----------------------------------------------------------------------------
# App & Security Configuration
# -----------------------------------------------------------------------------

app = Flask(__name__)

# Enforce secure headers (CSP, HSTS, X-Frame-Options, X-Content-Type-Options, etc.)
Talisman(app)

# Never run in debug mode in production
app.config['DEBUG'] = False

# Reject any request body larger than 1 KB
# (we only expect a small GET query string)
app.config['MAX_CONTENT_LENGTH'] = 1024  # bytes

# Configure logging (do not log sensitive info)
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s in %(module)s: %(message)s'
)

# Strict email validation regex (RFC 5322–lite)
EMAIL_REGEX = re.compile(
    r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$"
)

# -----------------------------------------------------------------------------
# Utility: Generic response to avoid leaking which emails exist
# -----------------------------------------------------------------------------
def _generic_unsubscribe_response():
    message = "If the email provided exists in our system, it has been unsubscribed."
    # We return plain-text (no HTML templates), so risk of XSS is minimal.
    # Further, we never reflect user input back into the response.
    return Response(message, mimetype='text/plain'), 200

# -----------------------------------------------------------------------------
# Utility: Safely fetch a DB connection using environment variables only
# -----------------------------------------------------------------------------
def _get_db_connection():
    try:
        host     = os.environ['DB_HOST']
        port     = int(os.environ.get('DB_PORT', '3306'))
        user     = os.environ['DB_USER']
        password = os.environ['DB_PASSWORD']
        database = os.environ['DB_NAME']
    except KeyError as e:
        logging.critical("Missing required environment variable: %s", e)
        # Fail fast if we're misconfigured
        raise RuntimeError("Server configuration error")

    # Ensure we only use Unicode, with an explicit charset
    return MySQLdb.connect(
        host=host,
        port=port,
        user=user,
        passwd=password,
        db=database,
        use_unicode=True,
        charset='utf8mb4',
        connect_timeout=5
    )

# -----------------------------------------------------------------------------
# Route: /unsubscribe
# -----------------------------------------------------------------------------
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Fetch and preliminarily clean the 'email' GET parameter
    email = request.args.get('email', default=None, type=str)
    if not email:
        # Missing or non-string → generic response
        return _generic_unsubscribe_response()

    email = email.strip()

    # 2) Validate length
    if len(email) > 254:
        # Too long to be a valid email address
        return _generic_unsubscribe_response()

    # 3) Validate format
    if not EMAIL_REGEX.fullmatch(email):
        # Bad format
        return _generic_unsubscribe_response()

    # 4) Attempt to unsubscribe in the database
    try:
        conn = _get_db_connection()
        cursor = conn.cursor()

        # Look up subscribers who have not yet been unsubscribed
        cursor.execute(
            "SELECT id FROM subscribers WHERE email = %s AND unsubscribed = 0",
            (email,)
        )
        row = cursor.fetchone()

        if row:
            subscriber_id = row[0]
            # Mark as unsubscribed
            cursor.execute(
                "UPDATE subscribers SET unsubscribed = 1 WHERE id = %s",
                (subscriber_id,)
            )
            conn.commit()

        cursor.close()
        conn.close()

    except Exception as ex:
        # Log the error internally, but never reveal details to the user
        logging.error("Database error in /unsubscribe: %s", ex)

    # 5) Always return the same generic message
    return _generic_unsubscribe_response()

# -----------------------------------------------------------------------------
# Healthcheck / index (optional)
# -----------------------------------------------------------------------------
@app.route('/')
def index():
    return (
        "Service is up. To unsubscribe, GET /unsubscribe?email=you@example.com\n",
        200,
        {'Content-Type': 'text/plain'}
    )
