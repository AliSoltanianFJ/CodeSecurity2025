from flask import Flask, request, jsonify
import os
import re
import logging
import MySQLdb
from MySQLdb import OperationalError, Error

app = Flask(__name__)

# Configure basic logging. In production you might route this to a file or centralized logger.
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Simple, conservative email‐format check.
EMAIL_REGEX = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")

# Load database configuration from environment variables.
# Ensure DB_USER, DB_PASS, DB_NAME are set in your deployment environment.
DB_CONFIG = {
    'host':     os.getenv('DB_HOST', 'localhost'),
    'user':     os.getenv('DB_USER'),
    'passwd':   os.getenv('DB_PASS'),
    'db':       os.getenv('DB_NAME'),
    'charset':  'utf8mb4',
    'use_unicode': True,
}

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Retrieve and normalize the input
    email = request.args.get('email', '').strip()
    if not email:
        return jsonify({'error': 'Missing email parameter.'}), 400

    # 2) Validate length and basic format
    if len(email) > 254 or not EMAIL_REGEX.match(email):
        return jsonify({'error': 'Invalid email address.'}), 400

    conn = None
    cursor = None

    try:
        # 3) Establish a new database connection
        conn = MySQLdb.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # 4) Perform the unsubscribe operation via a single parameterized DELETE
        #    This both removes the subscription if it exists and avoids a prior SELECT,
        #    cutting down on round-trips and side‐channel timing differences.
        delete_sql = "DELETE FROM subscribers WHERE email = %s"
        affected_rows = cursor.execute(delete_sql, (email,))

        # 5) If a row was deleted, commit; if not, no harm—still commit (or could skip).
        if affected_rows > 0:
            conn.commit()
        else:
            # Even if no rows matched, issue a commit to normalize timing
            conn.commit()

        # 6) Always return the same message to prevent user enumeration
        return jsonify({
            'message': 'If the email exists in our system, it has been unsubscribed.'
        }), 200

    except OperationalError:
        # Database server is unreachable or credentials invalid
        logger.exception('Database connection failed.')
        return jsonify({'error': 'Service temporarily unavailable.'}), 503

    except Error:
        # Catch all other database errors
        logger.exception('Unexpected database error.')
        return jsonify({'error': 'An unexpected error occurred.'}), 500

    finally:
        # 7) Clean up resources
        if cursor:
            try:
                cursor.close()
            except Exception:
                logger.warning('Failed to close cursor.', exc_info=True)
        if conn:
            try:
                conn.close()
            except Exception:
                logger.warning('Failed to close database connection.', exc_info=True)
