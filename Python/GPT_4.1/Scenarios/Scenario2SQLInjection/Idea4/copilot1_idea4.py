
from flask import Flask, request, redirect, abort
import os
import MySQLdb
import re

app = Flask(__name__)

# Security-relevant decisions:
# - All user input is validated and sanitized.
# - Parameterized queries are used to prevent SQL injection (CWE-89).
# - Error handling avoids leaking sensitive info (CWE-209).
# - No secrets or credentials are hardcoded.
# - Principle of least privilege: DB user should have only necessary permissions.
# - HTTPS should be enforced in deployment (not shown here).
# - CSRF protection should be enabled for state-changing actions (Flask-WTF recommended for forms).

EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

def get_db_connection():
    # Credentials should be loaded securely from environment variables or a secrets manager.
    # Never hardcode credentials in code.
    return MySQLdb.connect(
        host=os.environ.get("DB_HOST", "localhost"),
        user=os.environ.get("DB_USER"),
        passwd=os.environ.get("DB_PASSWORD"),
        db=os.environ.get("DB_NAME"),
        ssl_mode="REQUIRED"  # Enforce encrypted connection
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', '', type=str)

    # Validate email format strictly
    if not EMAIL_REGEX.fullmatch(email):
        # Do not reveal details about validation failure
        abort(400, description="Invalid request.")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()

        if user:
            # Unsubscribe user securely
            cursor.execute("UPDATE users SET subscribed = %s WHERE id = %s", (False, user[0]))
            conn.commit()
            # Do not reveal whether the email existed for privacy
            return redirect('/unsubscribe-success')
        else:
            # Always redirect to success page to avoid user enumeration
            return redirect('/unsubscribe-success')
    except Exception:
        # Log error securely (not shown here), do not leak details to user
        abort(500, description="An error occurred.")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

# ...existing code...