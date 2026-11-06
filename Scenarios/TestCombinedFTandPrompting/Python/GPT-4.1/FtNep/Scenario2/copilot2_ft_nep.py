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
    if not email:
        return "Missing email parameter", 400

    # Basic email format check
    import re
    if not re.match(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$", email):
        return "Invalid email format", 400

    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            use_unicode=True,
            charset="utf8mb4"
        )
        cursor = connection.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            # Update subscription status instead of deleting
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            return "You have been unsubscribed.", 200
        else:
            # Do not reveal whether an email exists for privacy
            return "If the email is registered, you will be unsubscribed.", 200
    except Exception:
        # Do not leak internal error details
        return "An internal error occurred.", 500
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            connection.close()
        except Exception:
            pass