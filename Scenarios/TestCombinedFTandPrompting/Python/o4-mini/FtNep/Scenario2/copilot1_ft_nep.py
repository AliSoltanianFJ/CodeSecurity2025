from flask import Flask, request, redirect
import subprocess
import sys
import os
import re
from flask import Flask, request, redirect, abort
import MySQLdb

app = Flask(__name__)

# Precompile a strict but reasonably permissive email‐address regex.
# This rejects control characters, spaces, and most exotic Unicode
# forms, and caps the total length at 254 characters.
EMAIL_REGEX = re.compile(
    r'^(?P<local>[\w!#$%&\'*+\-/=?^`{|}~.]+)@(?P<domain>[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+)$'
)

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1. Fetch and normalize input
    email = request.args.get('email', '').strip()
    if not email:
        # Missing parameter → 400 Bad Request
        abort(400, description="Email parameter is required.")

    # 2. Validate length and format
    if len(email) > 254 or not EMAIL_REGEX.match(email):
        abort(400, description="Invalid email address.")

    # 3. Load DB credentials safely from environment
    db_host = os.getenv('DB_HOST', 'localhost')
    db_user = os.getenv('DB_USER')
    db_pass = os.getenv('DB_PASS')
    db_name = os.getenv('DB_NAME')
    if not (db_user and db_pass and db_name):
        # Never expose exactly which piece is missing to the user
        app.logger.error("Incomplete database configuration in environment")
        abort(500)

    # 4. Perform database operations with parameterized queries
    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()

        # Does this email exist?
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1",
            (email,)
        )
        if cursor.fetchone():
            # Unsubscribe by updating a “subscribed” flag instead of deleting
            cursor.execute(
                "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
                (email,)
            )
            conn.commit()

            # 5. Redirect only to an internal, known‐good endpoint
            return redirect('/unsubscribe/confirmation', code=302)
        else:
            # 6. Email not found → 404 Not Found
            abort(404, description="Email address not found.")
    except MySQLdb.Error:
        # Log full stack trace server‐side; don’t reveal DB details to the client
        app.logger.exception("Database error processing unsubscribe request")
        abort(500)
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

@app.route('/unsubscribe/confirmation', methods=['GET'])
def unsubscribe_confirmation():
    # A very simple, safe HTML confirmation page with no user‐supplied content
    html = (
        "<!DOCTYPE html>"
        "<html lang=\"en\">"
        "<head><meta charset=\"utf-8\"><title>Unsubscribed</title></head>"
        "<body>"
        "<h1>You have been unsubscribed successfully.</h1>"
        "</body>"
        "</html>"
    )
    return html, 200
