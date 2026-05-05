import re
import logging
import os
import MySQLdb
from flask import Flask, request, abort, render_template

# ------------ Configuration & Setup ------------

app = Flask(__name__)

# Database configuration should come ONLY from environment variables
DB_CONFIG = {
    'host':   os.getenv('DB_HOST',   'localhost'),
    'user':   os.getenv('DB_USER'),
    'passwd': os.getenv('DB_PASS'),
    'db':     os.getenv('DB_NAME'),
    'connect_timeout': 5,
    # If you require SSL/TLS for MySQL, you can add:
    # 'ssl': {'ca': os.getenv('MYSQL_SSL_CA')}
}

# Only accept 32-char hexadecimal tokens
TOKEN_REGEX = re.compile(r'^[A-Fa-f0-9]{32}$')

# Set up simple logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s %(levelname)s %(message)s'
)


# ------------ Unsubscribe Endpoint ------------

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    """
    Secure unsubscribe by one-time token.  Does NOT reveal
    whether the token was valid or already used.
    """

    token = (request.args.get('token') or '').strip()
    if not TOKEN_REGEX.match(token):
        # Malformed or missing token: reject immediately
        abort(400)

    try:
        # Connect with very limited privileges (only SELECT/UPDATE on subscribers table)
        conn = MySQLdb.connect(**DB_CONFIG)
    except MySQLdb.Error as e:
        logging.error("DB connection failed: %s", e)
        # Do not reveal details to user
        abort(500)

    try:
        with conn:
            with conn.cursor() as cur:
                # Atomically mark unsubscribed if token is valid AND not already unsubscribed
                cur.execute(
                    """
                    UPDATE subscribers
                       SET unsubscribed     = 1,
                           unsubscribed_at  = NOW()
                     WHERE unsubscribe_token = %s
                       AND unsubscribed      = 0
                    """,
                    (token,)
                )
                rows_affected = cur.rowcount

    except MySQLdb.Error as e:
        logging.error("DB query failed: %s", e)
        abort(500)
    finally:
        conn.close()

    # Always render the same page, to avoid email enumeration
    return render_template('unsubscribe_complete.html'), 200


# ------------ Error Handlers (optional) ------------

@app.errorhandler(400)
def bad_request(e):
    return render_template('400.html'), 400

@app.errorhandler(500)
def server_error(e):
    return render_template('500.html'), 500