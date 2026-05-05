from flask import Flask, request, render_template, abort
import re
import os
import MySQLdb
import logging

app = Flask(__name__)

# -----------------------------------------------------------------------------
# Configuration
# -----------------------------------------------------------------------------
# It’s critical not to hard-code credentials.  Instead, load them from
# environment variables, a protected config file, or a secrets manager.
DB_CONFIG = {
    'host':     os.environ.get('DB_HOST',     'localhost'),
    'port':     int(os.environ.get('DB_PORT', 3306)),
    'user':     os.environ.get('DB_USER',     'appuser'),
    'passwd':   os.environ.get('DB_PASS',     None),     # must be set
    'db':       os.environ.get('DB_NAME',     'mailing'),
    'charset':  'utf8mb4',
    'cursorclass': MySQLdb.cursors.DictCursor
}

# Pre-compile a simple email‐validation regex:
EMAIL_RE = re.compile(r"^[^@]+@[^@]+\.[^@]+$")

# -----------------------------------------------------------------------------
# Helper: validate the email parameter
# -----------------------------------------------------------------------------
def is_valid_email(email):
    if not email or len(email) > 254:
        return False
    return EMAIL_RE.match(email) is not None

# -----------------------------------------------------------------------------
# Route: unsubscribe
# -----------------------------------------------------------------------------
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1. Extract and sanitize input
    email = request.args.get('email', '').strip()
    if not is_valid_email(email):
        # 400 Bad Request for invalid or missing email
        abort(400, description="Invalid email address.")

    try:
        # 2. Connect to the database using a short‐lived connection
        conn = MySQLdb.connect(**DB_CONFIG)
        cursor = conn.cursor()
        
        # 3. Use a parameterized query to avoid SQL injection
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s AND subscribed = 1 LIMIT 1",
            (email,)
        )
        row = cursor.fetchone()
        if not row:
            # 404 Not Found if the email is not in the list or already unsubscribed
            return render_template('unsubscribe_not_found.html', email=email), 404

        # 4. Perform the “unsubscribe” action
        cursor.execute(
            "UPDATE subscribers SET subscribed = 0, unsubscribed_at = NOW() WHERE email = %s",
            (email,)
        )
        conn.commit()

        # 5. Show a neutral success page
        return render_template('unsubscribe_success.html', email=email), 200

    except MySQLdb.Error:
        # 6. Log the full exception, but don’t reveal details to the user
        app.logger.exception("Database error during unsubscribe")
        abort(500, description="Internal server error. Please try again later.")

    finally:
        # 7. Always clean up resources
        try:
            cursor.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass

# -----------------------------------------------------------------------------
# Error handlers to avoid leaking stack traces in production
# -----------------------------------------------------------------------------
@app.errorhandler(400)
@app.errorhandler(404)
@app.errorhandler(500)
def error_page(error):
    # Render a generic template for client (4xx) or server (5xx) errors
    code = getattr(error, 'code', 500)
    message = getattr(error, 'description', 'An error occurred.')
    return render_template('error.html', code=code, message=message), code

# -----------------------------------------------------------------------------
# (Optional) Enable basic request logging for audit/troubleshooting
# -----------------------------------------------------------------------------
if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    # Never run Flask’s built‐in server in production!
    app.run(host='0.0.0.0', port=8080, debug=False)