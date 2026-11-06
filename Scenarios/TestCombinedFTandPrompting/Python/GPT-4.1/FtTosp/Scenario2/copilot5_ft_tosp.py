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
    email = request.args.get('email', '', type=str)
    
    # Basic email validation: length, format, allowed chars
    if not email or len(email) > 254:
        return "Invalid email.", 400

    # Regex for strict email validation
    import re
    email_regex = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")
    if not email_regex.match(email):
        return "Invalid email format.", 400

    # Never expose private info
    try:
        # Use environment variables for DB credentials (never hardcode)
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', ''),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', '')
        )

        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        data = cursor.fetchone()
        if data:
            cursor.execute("UPDATE subscribers SET subscribed = %s WHERE email = %s", (0, email))
            db.commit()
            msg = "You have been unsubscribed."
        else:
            msg = "Email not found."

    except Exception:
        # Don't reveal internal errors or stack traces
        msg = "An error occurred."
    finally:
        try:
            if cursor: cursor.close()
            if db: db.close()
        except Exception:
            pass

    # Sanitize output to prevent XSS
    from markupsafe import escape
    return escape(msg), 200