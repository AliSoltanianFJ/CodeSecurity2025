import os
import re
import logging
import sys

from flask import Flask, request, abort, render_template_string
from markupsafe import escape
import MySQLdb
from MySQLdb.cursors import DictCursor

#
# Configuration from environment (no hard-coded secrets)
#
DB_HOST        = os.getenv('DB_HOST', 'localhost')
DB_PORT        = int(os.getenv('DB_PORT', '3306'))
DB_USER        = os.getenv('DB_USER')
DB_PASSWORD    = os.getenv('DB_PASSWORD')
DB_NAME        = os.getenv('DB_NAME')
MYSQL_CA_CERT  = os.getenv('MYSQL_CA_CERT')  # optional: path to CA cert for SSL
APP_PORT       = int(os.getenv('PORT', '5000'))

# Fail fast if mandatory variables are missing
if not all([DB_USER, DB_PASSWORD, DB_NAME]):
    logging.critical("One or more required DB environment variables are not set (DB_USER, DB_PASSWORD, DB_NAME).")
    sys.exit(1)

# Initialize Flask
app = Flask(__name__)
app.config['DEBUG'] = False
app.config['PROPAGATE_EXCEPTIONS'] = False

# Simple, strict email pattern (enough for most use cases)
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]{1,64}@'      # local part: up to 64 chars
                         r'[A-Za-z0-9.-]+\.'              # domain + dot
                         r'[A-Za-z]{2,63}$')              # TLD

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s %(levelname)s %(message)s'
)

#
# Error handlers: never expose stack traces or internal errors to clients
#
@app.errorhandler(400)
def handle_400(error):
    return "Bad request.", 400

@app.errorhandler(404)
def handle_404(error):
    return "Page not found.", 404

@app.errorhandler(500)
def handle_500(error):
    logging.error("Internal server error: %s", error)
    return "Internal server error.", 500

#
# Helper: get a secure, parameterized MySQLdb connection
#
def get_db_connection():
    ssl_kwargs = {}
    if MYSQL_CA_CERT:
        ssl_kwargs['ca'] = MYSQL_CA_CERT

    return MySQLdb.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        charset='utf8mb4',
        cursorclass=DictCursor,
        ssl=ssl_kwargs or None
    )

#
# /unsubscribe endpoint
#   Expects GET parameter "email"
#   Validates format and length, looks up in DB, marks as unsubscribed
#
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Retrieve and type-cast
    email = request.args.get('email', '', type=str)

    # 2) Basic presence and length checks
    if not email:
        abort(400)
    if len(email) > 254:
        abort(400)

    # 3) Strict regex validation
    if not EMAIL_REGEX.match(email):
        abort(400)

    try:
        # 4) Database operations with parameterized queries
        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                # Check if this email exists in the subscribers table
                cursor.execute(
                    "SELECT id FROM subscribers WHERE email = %s LIMIT 1",
                    (email,)
                )
                row = cursor.fetchone()

                if not row:
                    # We do not reveal whether the email was subscribed or not.
                    # This avoids account enumeration.
                    msg = ("If your email was subscribed, you have now been unsubscribed. "
                           "Thank you.")
                    return render_template_string("<p>{{ message }}</p>",
                                                  message=msg), 200

                # Mark as unsubscribed (or delete, as your schema requires)
                cursor.execute(
                    "UPDATE subscribers "
                    "SET unsubscribed = 1, unsubscribed_at = NOW() "
                    "WHERE id = %s",
                    (row['id'],)
                )
                conn.commit()

        # 5) Escape any user data before returning
        safe_email = escape(email)
        return render_template_string(
            "<p>The email <strong>{{ email }}</strong> has been successfully unsubscribed.</p>",
            email=safe_email
        ), 200

    except MySQLdb.Error:
        logging.exception("Database error on unsubscribe request")
        abort(500)
    except Exception:
        logging.exception("Unexpected error on unsubscribe request")
        abort(500)
