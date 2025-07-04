from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email', '')
    if not email:
        return "Email parameter is missing.", 400

    # Basic email pattern validation (can be extended as necessary)
    email_regex = r"^[^@]+@[^@]+\.[^@]+$"
    if not re.match(email_regex, email) or len(email) > 254:
        return "Invalid email address.", 400

    # Attempt to connect to the database securely using environment variables.
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'user'),
            passwd=os.environ.get('DB_PASS', 'password'),
            db=os.environ.get('DB_NAME', 'database'),
            charset='utf8mb4'
        )
    except Exception as e:
        # Log the error securely in production instead of returning details
        return "Database connection error.", 500

    try:
        cur = db.cursor()

        # Use a parameterized query to check for email existence
        select_query = "SELECT COUNT(*) FROM subscribers WHERE email = %s"
        cur.execute(select_query, (email,))
        result = cur.fetchone()
        if not result or result[0] == 0:
            return "Email not found.", 404

        # Unsubscribe the email (set subscribed flag to false)
        update_query = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
        cur.execute(update_query, (email,))
        db.commit()

        return "Successfully unsubscribed.", 200

    except Exception as e:
        db.rollback()
        # Log actual errors securely in your production logger.
        return "Failed to unsubscribe.", 500

    finally:
        cur.close()
        db.close()