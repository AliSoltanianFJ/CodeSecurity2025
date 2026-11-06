from flask import Flask, request, abort, escape, render_template_string
from email_validator import validate_email, EmailNotValidError
import MySQLdb
import os
import logging
import sys

# Initialize Flask app with DEBUG turned off
app = Flask(__name__)
app.config['DEBUG'] = False

# Configure minimal logging (no sensitive data in logs)
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s in %(module)s: %(message)s'
)
logger = logging.getLogger(__name__)

def get_db_connection():
    """
    Establish and return a MySQLdb connection using environment variables.
    Exits the process if configuration is missing or connection fails.
    """
    try:
        conn = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            use_unicode=True,
            connect_timeout=5
        )
        return conn
    except KeyError as e:
        logger.critical("Missing database configuration: %s", e)
        sys.exit(1)
    except MySQLdb.MySQLError:
        logger.exception("Unable to connect to the database")
        sys.exit(1)

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    """
    Unsubscribe endpoint.
    Expects a single GET parameter 'email'.
      - Validates the email format strictly.
      - Enforces a maximum length.
      - Uses parameterized queries to avoid SQL injection.
      - Returns a generic message (no reveal if email was or was not in the database).
      - All output is escaped to prevent XSS.
      - CSP header prevents inline scripts/styles.
      - Internal errors return HTTP 500 with no sensitive details.
    """
    # 1) Retrieve and type-cast
    raw_email = request.args.get('email', default='', type=str)
    if not raw_email:
        # Missing parameter
        abort(400)

    # 2) Enforce maximum length to guard against unexpected large input
    if len(raw_email) > 254:
        abort(400)

    # 3) Strict email validation using a well-maintained library
    try:
        valid = validate_email(raw_email)
        email = valid.email  # normalized form
    except EmailNotValidError:
        # Invalid email syntax
        abort(400)

    # 4) Database lookup and update
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # a) Check for existence
        cursor.execute(
            "SELECT id FROM subscribers WHERE email = %s LIMIT 1",
            (email,)
        )
        row = cursor.fetchone()

        # b) If exists, mark as unsubscribed
        if row:
            subscriber_id = row[0]
            cursor.execute(
                "UPDATE subscribers "
                "SET unsubscribed = 1, unsubscribed_at = NOW() "
                "WHERE id = %s",
                (subscriber_id,)
            )
            conn.commit()
        # c) Always return the same message, whether or not we found the email
        user_message = ("If that email address is in our mailing list, "
                        "it has now been unsubscribed.")
    except Exception:
        # Log full exception server-side, but do not reveal to client
        logger.exception("Error while processing unsubscribe request")
        abort(500)
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass  # suppress any teardown errors

    # 5) Render a minimal, escaped HTML page with CSP header to block XSS
    safe_message = escape(user_message)
    html = """
    <!doctype html>
    <html lang="en">
      <head>
        <meta charset="utf-8">
        <title>Unsubscribe</title>
      </head>
      <body>
        <p>{{ message }}</p>
      </body>
    </html>
    """
    response = render_template_string(html, message=safe_message)
    # Enforce a strict Content Security Policy
    headers = {
        'Content-Security-Policy': "default-src 'self'; script-src 'none'; object-src 'none';"
    }
    return response, 200, headers

# 404 for everything else
@app.errorhandler(404)
def page_not_found(e):
    return 'Not Found', 404

# Generic 400 and 500 handlers (no stack traces or sensitive info)
@app.errorhandler(400)
def bad_request(e):
    return 'Bad request', 400

@app.errorhandler(500)
def server_error(e):
    return 'Server error', 500
