from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Step 1: Validate and sanitize input
    email = request.args.get('email', type=str, default='').strip()
    if not email:
        return "Missing email parameter", 400
    if len(email) > 254:
        return "Email too long", 400
    import re
    email_regex = r"^[A-Za-z0-9\._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
    if not re.match(email_regex, email):
        return "Invalid email format", 400

    # Step 2: Use parameterized queries to prevent SQL injection
    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            charset='utf8mb4'
        )
        cursor = connection.cursor()

        # Step 3: Check existence securely
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if not result:
            return "Email not found", 404

        # Step 4: Unsubscribe securely (e.g., update flag, not delete)
        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
        connection.commit()
    except Exception:
        app.logger.exception("Error unsubscribing email")
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            connection.close()
        except Exception:
            pass

    # Step 5: Use generic message to avoid information leakage
    return "If your email was subscribed, it has been unsubscribed.", 200