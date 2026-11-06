from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400

    # Basic email format validation (length + regex)
    if len(email) > 254:
        return "Invalid email length", 400
    import re
    email_regex = r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$"
    if not re.match(email_regex, email):
        return "Invalid email format", 400

    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            charset="utf8mb4"
        )
        cursor = connection.cursor()
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        subscriber = cursor.fetchone()
        if subscriber:
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            return "Successfully unsubscribed", 200
        else:
            return "Email not found", 404
    except Exception:
        # Do not leak error details
        return "Server error", 500
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            connection.close()
        except Exception:
            pass