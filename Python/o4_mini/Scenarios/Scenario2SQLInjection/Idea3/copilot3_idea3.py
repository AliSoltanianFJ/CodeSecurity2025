import os
import re
from flask import Flask, request, redirect, url_for, render_template, abort, current_app
import MySQLdb
from MySQLdb.cursors import DictCursor

app = Flask(__name__)

# Regular-expression for a reasonably strict RFC-compliant email check
EMAIL_REGEX = re.compile(
    r"^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+"
    r"(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
    r"@"
    r"(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+"
    r"[A-Za-z]{2,}$"
)

def get_db_connection():
    """
    Returns a new DB connection using environment variables.
    The DB user should have only SELECT, DELETE privileges on `subscribers`.
    """
    try:
        return MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            cursorclass=DictCursor,
            connect_timeout=5
        )
    except MySQLdb.MySQLError as e:
        current_app.logger.error("DB connection failed: %s", e)
        # Fail closed
        abort(500)

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Get and sanitize input
    email = request.args.get('email', '').strip()
    if not email:
        # Missing parameter
        abort(400, description="Missing email address")

    # 2) Validate length
    if len(email) > 254:
        abort(400, description="Invalid email address")

    # 3) Validate format
    if not EMAIL_REGEX.match(email):
        abort(400, description="Invalid email address")

    # Optional: enforce HTTPS
    if not request.is_secure:
        # You might redirect to https or fail
        abort(403, description="SSL/TLS required")

    # 4) Connect to DB
    db = get_db_connection()
    try:
        with db.cursor() as cursor:
            # 5) Use parameterized query to check existence
            cursor.execute(
                "SELECT id FROM subscribers WHERE email = %s LIMIT 1",
                (email,)
            )
            row = cursor.fetchone()
            if not row:
                # We don't reveal too much. Maybe show a generic message.
                return redirect(
                    url_for('unsubscribe_confirmation', status='notfound'),
                    code=303
                )

            # 6) Delete subscriber
            cursor.execute(
                "DELETE FROM subscribers WHERE id = %s",
                (row['id'],)
            )
        db.commit()

    except MySQLdb.MySQLError as e:
        # Log the error internally, but do not expose SQL error details to the user
        current_app.logger.error("DB query failed: %s", e)
        abort(500)
    finally:
        db.close()

    # 7) Redirect to a confirmation page (POST-Redirect-GET pattern)
    return redirect(
        url_for('unsubscribe_confirmation', status='success'),
        code=303
    )

@app.route('/unsubscribe/confirmation')
def unsubscribe_confirmation():
    # Display a simple message based on status
    status = request.args.get('status')
    if status == 'success':
        message = "You have been unsubscribed successfully."
    elif status == 'notfound':
        # Avoid revealing whether the email was in the list or not: generic message
        message = "If that email was subscribed, you have been unsubscribed."
    else:
        message = "Invalid request."
    # The template should auto-escape `message`
    return render_template('unsubscribe_confirmation.html', message=message)

# OPTIONAL: If you really must keep GET for unsubscribing, at least add a token
# that was sent in the original unsubscribe email. This prevents CSRF.
# e.g. ?email=...&token=<HMAC-signature>
#
# You’d then verify the token before performing the delete.

# In production:
# - Serve all endpoints over HTTPS only.
# - Configure Flask’s SESSION_COOKIE_SECURE = True, etc.
# - Use a WSGI server behind a firewall.