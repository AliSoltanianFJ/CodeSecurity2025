from flask import Flask, request, jsonify
from email_validator import validate_email, EmailNotValidError
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
import MySQLdb
import os
import logging

# Initialize Flask
app = Flask(__name__)

# Configure basic logging to a file or stdout; in production send to a centralized logger
logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s %(message)s",
                    handlers=[logging.StreamHandler()])

# Rate limiter: limit each IP to 5 unsubscribe attempts per minute
limiter = Limiter(
    app,
    key_func=get_remote_address,
    default_limits=[]
)
unsubscribe_rate = "5 per minute"

# Read database configuration from environment variables
DB_CONFIG = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'user': os.environ.get('DB_USER', None),
    'passwd': os.environ.get('DB_PASS', None),
    'db': os.environ.get('DB_NAME', None),
    'charset': 'utf8mb4',
    'use_unicode': True,
    'connect_timeout': 5
}

# Ensure required variables are set
if not all([DB_CONFIG['user'], DB_CONFIG['passwd'], DB_CONFIG['db']]):
    logging.critical("Database credentials are not fully set in environment variables.")
    raise RuntimeError("Missing database configuration")

def get_db_connection():
    """
    Opens a new database connection using the parameters in DB_CONFIG.
    Caller is responsible for closing it.
    """
    return MySQLdb.connect(
        host=DB_CONFIG['host'],
        user=DB_CONFIG['user'],
        passwd=DB_CONFIG['passwd'],
        db=DB_CONFIG['db'],
        charset=DB_CONFIG['charset'],
        use_unicode=DB_CONFIG['use_unicode'],
        connect_timeout=DB_CONFIG['connect_timeout']
    )

@app.route('/unsubscribe', methods=['GET'])
@limiter.limit(unsubscribe_rate)
def unsubscribe():
    """
    Unsubscribe endpoint.
    Expects a single GET parameter: email.
    Does not reveal whether the address was subscribed.
    """
    raw_email = request.args.get('email', '').strip()
    if not raw_email:
        # Missing parameter
        return jsonify({"error": "Email parameter is required"}), 400

    # Validate email format and length
    try:
        # The validate_email() call normalizes and rejects invalid strings
        validated = validate_email(raw_email)
        email = validated.email  # e.g. lowercased, trimmed, IDNA-normalized
    except EmailNotValidError:
        # Invalid email address
        return jsonify({"error": "Invalid email address"}), 400

    # Enforce max length post-validation
    if len(email) > 254:
        return jsonify({"error": "Invalid email address"}), 400

    # Perform unsubscribe operation
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # Use a single parameterized DELETE statement
        # We do not first SELECT, to avoid timing differences and extra round-trips
        delete_sql = "DELETE FROM subscribers WHERE email = %s"
        cursor.execute(delete_sql, (email,))
        conn.commit()
        # cursor.rowcount tells us how many rows were affected, but we do not use that
        # in the response, to avoid disclosing whether the address existed.
    except MySQLdb.Error as db_err:
        # Log details internally, but do not send them to the client
        logging.error("Database error during unsubscribe for %s: %s", email, db_err)
        return jsonify({"error": "Unable to process request at this time"}), 500
    except Exception as ex:
        logging.exception("Unexpected error during unsubscribe for %s", email)
        return jsonify({"error": "Unable to process request at this time"}), 500
    finally:
        # Clean up resources
        try:
            if 'cursor' in locals() and cursor:
                cursor.close()
        except Exception:
            pass
        try:
            if 'conn' in locals() and conn:
                conn.close()
        except Exception:
            pass

    # Always return the same generic message
    return jsonify({
        "message": "If this email address was subscribed, it has now been unsubscribed."
    }), 200
