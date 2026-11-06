import os
import logging
from email.utils import parseaddr
from flask import Flask, request, jsonify
import MySQLdb
import MySQLdb.cursors

# -----------------------------------------------------------------------------
# Configuration
# -----------------------------------------------------------------------------
# Read database credentials (and port) from environment variables.
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', '3306'))
DB_USER = os.getenv('DB_USER', 'root')
DB_PASS = os.getenv('DB_PASS', '')
DB_NAME = os.getenv('DB_NAME', 'your_database')

# Configure logging: write to stderr, include timestamps and severity.
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s %(levelname)s %(name)s: %(message)s'
)
logger = logging.getLogger(__name__)

# -----------------------------------------------------------------------------
# Flask application setup
# -----------------------------------------------------------------------------
app = Flask(__name__)
# Always disable debug mode in production.
app.config['DEBUG'] = False
app.config['TESTING'] = False

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------
def is_valid_email(email: str) -> bool:
    """
    Quick validation of email format. This will not catch every invalid address
    (for that you’d need a full RFC-compliant parser), but it filters out
    empty strings, very long inputs, and anything without a user@domain part.
    """
    if not email or len(email) > 254:
        return False
    realname, addr = parseaddr(email)
    if '@' not in addr:
        return False
    # Basic domain‐and‐localchecks; no control characters, no spaces:
    if any(c.isspace() for c in addr):
        return False
    return True

# -----------------------------------------------------------------------------
# Route: /unsubscribe
# -----------------------------------------------------------------------------
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1. Extract and normalize the email parameter
    email = request.args.get('email', '').strip().lower()
    if not email:
        return jsonify({ 'error': 'Email parameter is required.' }), 400

    # 2. Validate its format
    if not is_valid_email(email):
        return jsonify({ 'error': 'Invalid email address.' }), 400

    conn = None
    cursor = None

    try:
        # 3. Establish a connection using a server-side cursor to minimize memory
        conn = MySQLdb.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            passwd=DB_PASS,
            db=DB_NAME,
            cursorclass=MySQLdb.cursors.DictCursor,
            connect_timeout=5,
            ssl={}  # Optional: configure SSL parameters if your DB supports it
        )
        cursor = conn.cursor()

        # 4. Check existence
        select_sql = """
            SELECT 1
              FROM subscribers
             WHERE email = %s
             LIMIT 1
        """
        cursor.execute(select_sql, (email,))
        row = cursor.fetchone()
        if not row:
            # No such subscriber
            return jsonify({ 'message': 'Email address not found.' }), 404

        # 5. Perform the unsubscribe action
        #    We choose DELETE here. If you prefer to keep an audit trail,
        #    switch to an UPDATE of a “subscribed” flag instead.
        delete_sql = """
            DELETE FROM subscribers
             WHERE email = %s
        """
        cursor.execute(delete_sql, (email,))
        conn.commit()

        return jsonify({ 'message': 'You have been successfully unsubscribed.' }), 200

    except MySQLdb.MySQLError:
        # Log full exception, but don’t reveal details to the client
        logger.exception("Database error while attempting to unsubscribe %r", email)
        return jsonify({ 'error': 'Internal server error.' }), 500

    except Exception:
        # Catch any other unexpected errors
        logger.exception("Unexpected error in /unsubscribe for %r", email)
        return jsonify({ 'error': 'Internal server error.' }), 500

    finally:
        # Clean up database resources
        if cursor is not None:
            try:
                cursor.close()
            except Exception:
                logger.warning("Failed to close cursor", exc_info=True)
        if conn is not None:
            try:
                conn.close()
            except Exception:
                logger.warning("Failed to close database connection", exc_info=True)

