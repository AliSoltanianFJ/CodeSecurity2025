import re
import os
from flask import Flask, request, redirect, url_for, abort, render_template_string
import MySQLdb
import logging

# -----------------------------------------------------------------------------
# Configuration – pull all secrets from environment or a secrets manager
# -----------------------------------------------------------------------------
DB_HOST     = os.getenv('DB_HOST', 'localhost')
DB_PORT     = int(os.getenv('DB_PORT', '3306'))
DB_USER     = os.getenv('DB_USER', 'appuser')
DB_PASS     = os.getenv('DB_PASS', '')
DB_NAME     = os.getenv('DB_NAME', 'mailinglist')

# A simple email‐validation regex. Adjust to your site’s policy.
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

app = Flask(__name__)
app.logger.setLevel(logging.INFO)

# Simple HTML template for all unsubscribe outcomes.
UNSUBSCRIBE_HTML = """
<!doctype html>
<title>Unsubscribe</title>
<h1>Unsubscribe Request</h1>
<p>{{ message }}</p>
"""

def get_db_connection():
    """
    Returns a new MySQLdb connection with UTF-8 encoding.
    Raises MySQLdb.Error on failure.
    """
    return MySQLdb.connect(
        host    = DB_HOST,
        port    = DB_PORT,
        user    = DB_USER,
        passwd  = DB_PASS,
        db      = DB_NAME,
        charset = 'utf8mb4',
        use_unicode = True
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Get and validate the email parameter
    email = request.args.get('email', '').strip()
    if not email:
        return render_template_string(UNSUBSCRIBE_HTML,
                                      message="Missing email parameter."), 400

    if not EMAIL_REGEX.fullmatch(email):
        app.logger.info("Invalid email format on unsubscribe")
        # Generic response to avoid leaking whether the format was wrong vs. not in DB
        return render_template_string(UNSUBSCRIBE_HTML,
                                      message="If that address is subscribed, it has now been removed."), 200

    # 2) Connect to the database
    try:
        conn = get_db_connection()
    except MySQLdb.Error as e:
        app.logger.error("Database connection failed: %s", e)
        # Do not leak details to the user
        abort(500, description="Internal server error")

    try:
        cursor = conn.cursor()

        # 3) Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s AND unsubscribed = 0", (email,))
        row = cursor.fetchone()

        if row:
            # 4) Mark as unsubscribed instead of deleting, for audit
            cursor.execute("UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = NOW() WHERE id = %s", (row[0],))
            conn.commit()
            app.logger.info("Email unsubscribed.")
        else:
            # Either not found, or already unsubscribed
            app.logger.info("Unsubscribe called for non-existent or already unsubscribed email")

    except MySQLdb.Error as e:
        app.logger.error("Database query failed: %s", e)
        abort(500, description="Internal server error")
    finally:
        cursor.close()
        conn.close()

    # 5) Always return the same generic message to avoid user-enumeration
    return render_template_string(UNSUBSCRIBE_HTML,
                                  message="If that address is subscribed, it has now been removed."), 200

if __name__ == '__main__':
    # In production, run under gunicorn/uwsgi/etc., not Flask’s dev server
    app.run(host='0.0.0.0', port=5000, debug=False)