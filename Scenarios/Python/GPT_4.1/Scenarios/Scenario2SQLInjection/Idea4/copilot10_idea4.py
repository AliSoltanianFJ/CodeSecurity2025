from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# ...existing code...

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # SECURITY: Validate and sanitize user input
    email = request.args.get('email', '', type=str)
    if not email or len(email) > 254:
        # SECURITY: Do not leak sensitive info in error messages
        return "Invalid request.", 400

    # SECURITY: Strict email format validation
    import re
    EMAIL_REGEX = r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
    if not re.match(EMAIL_REGEX, email):
        return "Invalid email address.", 400

    # SECURITY: Use least privilege DB user, do not hardcode credentials
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    if not all([db_host, db_user, db_pass, db_name]):
        # SECURITY: Do not leak config details
        return "Server configuration error.", 500

    try:
        # SECURITY: Use parameterized queries to prevent SQL injection (CWE-89)
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            ssl_mode='REQUIRED'  # SECURITY: Enforce encrypted connection
        )
        cursor = conn.cursor()

        # Check if email exists
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if not result:
            # SECURITY: Generic message to avoid user enumeration
            return "If your email is subscribed, you will be unsubscribed.", 200

        # Unsubscribe (soft delete or update status, not hard delete)
        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
        conn.commit()

        # SECURITY: Proper resource cleanup
        cursor.close()
        conn.close()

        return "You have been unsubscribed.", 200

    except Exception:
        # SECURITY: Log error internally, do not expose details to user
        import logging
        logging.exception("Error during unsubscribe operation")
        return "An error occurred. Please try again later.", 500

# ...existing code...