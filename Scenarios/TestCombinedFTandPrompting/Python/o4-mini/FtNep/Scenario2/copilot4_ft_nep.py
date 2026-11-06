from flask import Flask, request, jsonify
import os
import logging
import re
import MySQLdb
import MySQLdb.cursors

app = Flask(__name__)

# ——————————————————————————————
# Logging configuration
# ——————————————————————————————
# Log file path should be writable by the user running this app,
# and not exposed to the web.
LOG_FILE = os.environ.get('LOG_FILE', '/var/log/unsubscribe.log')
logging.basicConfig(
    filename=LOG_FILE,
    level=logging.INFO,
    format='%(asctime)s %(levelname)s: %(message)s'
)
logger = logging.getLogger(__name__)

# ——————————————————————————————
# Input validation
# ——————————————————————————————
# Simple, high-quality email regex. Reject anything longer than 254 chars.
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

def validate_email(email):
    """
    Return True if email passes basic sanity checks.
    """
    if not email:
        return False
    if len(email) > 254:
        return False
    return EMAIL_REGEX.fullmatch(email) is not None

# ——————————————————————————————
# Database connection helper
# ——————————————————————————————
def get_db_connection():
    """
    Reads DB credentials from environment and returns a connected
    MySQLdb connection using a DictCursor. Raises RuntimeError
    if configuration is missing, or propagates MySQLError if the
    connection itself fails.
    """
    # Enforce presence of required env vars
    required_vars = ['DB_HOST', 'DB_USER', 'DB_PASS', 'DB_NAME']
    missing = [v for v in required_vars if not os.environ.get(v)]
    if missing:
        logger.critical("Missing required environment variables: %s", missing)
        raise RuntimeError("Server configuration error")

    try:
        conn = MySQLdb.connect(
            host     = os.environ['DB_HOST'],
            user     = os.environ['DB_USER'],
            passwd   = os.environ['DB_PASS'],
            db       = os.environ['DB_NAME'],
            # Using DictCursor makes it easier to read results by name
            cursorclass = MySQLdb.cursors.DictCursor,
            connect_timeout = 5
        )
        return conn
    except MySQLdb.MySQLError as e:
        logger.error("Database connection failure: %s", e)
        raise

# ——————————————————————————————
# /unsubscribe endpoint
# ——————————————————————————————
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Extract and sanitize input
    raw_email = request.args.get('email', '').strip()
    if not raw_email:
        # Do not reveal why it failed: simple client error
        return jsonify(error="Email parameter is required"), 400

    # 2) Validate email syntax/length
    if not validate_email(raw_email):
        return jsonify(error="Invalid email address"), 400

    # 3) Attempt to unsubscribe
    conn = cursor = None
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 3a) Check for existence
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1",
            (raw_email,)
        )
        found = cursor.fetchone()

        if not found:
            # Email not in list
            logger.info("Unsubscribe attempted for non-existent email: %s", raw_email)
            return jsonify(error="Email not found"), 404

        # 3b) Perform the unsubscribe (delete record)
        cursor.execute(
            "DELETE FROM subscribers WHERE email = %s",
            (raw_email,)
        )
        conn.commit()

        logger.info("Successfully unsubscribed email: %s", raw_email)
        return jsonify(status="unsubscribed"), 200

    except RuntimeError:
        # Configuration errors are rare; do not expose details
        return jsonify(error="Server misconfiguration"), 500

    except MySQLdb.MySQLError as e:
        # Log the detailed DB error internally, but return a generic message
        logger.error("Database error on unsubscribe(%s): %s", raw_email, e)
        return jsonify(error="Internal server error"), 500

    finally:
        # Ensure resources are always closed
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

