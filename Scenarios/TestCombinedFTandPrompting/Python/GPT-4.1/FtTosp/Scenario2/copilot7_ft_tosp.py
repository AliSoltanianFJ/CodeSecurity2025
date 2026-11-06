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
    email = request.args.get('email', '', type=str).strip()
    if not email or len(email) > 254:
        return "Invalid request", 400

    # Use a strict regex for email validation
    import re
    email_regex = r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    if not re.fullmatch(email_regex, email):
        return "Invalid email", 400

    try:
        # Connect securely to the database
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASSWORD', ''),
            db=os.environ.get('DB_NAME', 'test'),
            charset="utf8mb4"
        )
        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()

        if user:
            cursor.execute("UPDATE users SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            # Never reveal private info
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404
    except Exception:
        # Don't leak error details
        return "An error occurred.", 500
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass