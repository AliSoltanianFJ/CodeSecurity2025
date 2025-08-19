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
    # Retrieve and clean the email parameter
    email = request.args.get("email", "").strip()
    if not email:
        return "Email parameter missing or empty.", 400

    # Validate email address format
    email_regex = r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
    if not re.fullmatch(email_regex, email):
        return "Invalid email address.", 400

    try:
        # Connect to the database using UTF-8 charset; update credentials as needed
        db = MySQLdb.connect(
            host="your_host", 
            user="your_username", 
            passwd="your_password", 
            db="your_database", 
            charset="utf8mb4"
        )
        cursor = db.cursor()

        # Use a parameterized query to check if the email exists
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result:
            # Email exists; update the user's subscription status safely
            update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404

    except Exception:
        # Generic error message; do not expose internal errors to the user
        return "An error occurred.", 500

    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass