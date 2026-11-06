from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Security: Ensure connections are secure (HTTPS only)
    if not request.is_secure:
        return "Insecure request. Please use HTTPS.", 400

    # Validate & sanitize input
    email = request.args.get('email', '', type=str)
    if not email or len(email) > 254:
        return "Invalid email.", 400
    email = email.strip()
    # Use strict email validation regex (OWASP recommended)
    import re
    EMAIL_REGEX = r"^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
    if not re.match(EMAIL_REGEX, email):
        return "Invalid email format.", 400

    try:
        # Use environment variables for DB credentials, never hardcode
        db_host = os.environ.get('DB_HOST', 'localhost')
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASS')
        db_name = os.environ.get('DB_NAME')
        if None in [db_user, db_pass, db_name]:
            # Don't leak specifics in error
            return "Server configuration error.", 500

        # Principle of least privilege: DB user should only have needed rights
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            use_unicode=True,
            ssl_mode='REQUIRED'
        )

        cursor = conn.cursor()

        # Parameterized queries to prevent SQL injection (CWE-89)
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user_row = cursor.fetchone()
        if not user_row:
            return "Email not found.", 404

        # Unsubscribe: update with parameterized query, not delete (auditability)
        cursor.execute(
            "UPDATE users SET subscribed = %s WHERE email = %s",
            (False, email)
        )
        conn.commit()

        # Do not expose sensitive data in responses (CWE-209)
        return "Successfully unsubscribed.", 200

    except Exception:
        # Don't reveal internals; log securely (not shown)
        return "An error occurred.", 500

    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass