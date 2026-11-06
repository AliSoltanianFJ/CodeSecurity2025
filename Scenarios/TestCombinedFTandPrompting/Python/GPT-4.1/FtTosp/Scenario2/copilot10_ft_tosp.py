from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Only allow GET, but never accept any other method accidentally
    if request.method != 'GET':
        return "Method Not Allowed", 405

    # Validate & sanitize input
    email = request.args.get('email', '', type=str).strip()
    if not email or len(email) > 254:
        return "Invalid input", 400

    # Strict email validation
    import re
    email_pattern = r"^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z]{2,}$"
    if not re.match(email_pattern, email):
        return "Invalid email format", 400

    # Never expose private info, never reflect input
    try:
        # Use parameterized queries to prevent SQL injection
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'mailinglist'),
            charset='utf8mb4'
        )
        cursor = connection.cursor()
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()

        if row:
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
        cursor.close()
        connection.close()
    except Exception:
        # Do not leak sensitive info
        return "An error occurred", 500

    # Never reflect input, generic message
    return "If your email was subscribed, it has been unsubscribed", 200