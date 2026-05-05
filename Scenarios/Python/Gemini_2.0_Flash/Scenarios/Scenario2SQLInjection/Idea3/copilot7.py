from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Get email from query string
    email = request.args.get('email', '').strip()
    # Validate email format (basic regex, more comprehensive check is ideal)
    email_regex = r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$"
    import re
    if not email or not re.match(email_regex, email):
        return "Invalid or missing email.", 400

    try:
        # Connect to DB (use environment variables for credentials)
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'mydatabase')
        )
        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404
    except Exception as e:
        print("Error unsubscribing:", e)
        return "An error occurred.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass