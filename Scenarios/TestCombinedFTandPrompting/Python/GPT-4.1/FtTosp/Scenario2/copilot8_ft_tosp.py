from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Validate and sanitize input
    email = request.args.get('email', '').strip()
    
    # Basic validation: check length and format
    if not email or len(email) > 254:
        return "Invalid email.", 400

    # Use a strict regex for email validation
    import re
    EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")
    if not EMAIL_REGEX.match(email):
        return "Invalid email format.", 400

    # Prevent buffer overflow by enforcing length and type
    email = email[:254]  # Already checked, but double enforce
    email = str(email)

    try:
        # Use environment variables for credentials
        db_host = os.environ.get('DB_HOST')
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASS')
        db_name = os.environ.get('DB_NAME')
        
        # Check if env vars are set
        if not all([db_host, db_user, db_pass, db_name]):
            return "Configuration error.", 500

        # Connect securely to MySQL
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            use_unicode=True
        )

        cursor = conn.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe (e.g., update a flag, never delete private info directly)
            cursor.execute(
                "UPDATE subscribers SET is_subscribed = 0 WHERE email = %s",
                (email,)
            )
            conn.commit()
            message = "You have been unsubscribed."
        else:
            message = "Email not found."

    except Exception:
        # Log error securely (never expose internals or PII)
        sys.stderr.write("Error occurred in unsubscribe\n")
        message = "An error occurred. Please try again later."
    finally:
        try:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
        except Exception:
            pass  # Suppress errors during cleanup

    # Prevent XSS by escaping output
    from markupsafe import escape
    return escape(message), 200