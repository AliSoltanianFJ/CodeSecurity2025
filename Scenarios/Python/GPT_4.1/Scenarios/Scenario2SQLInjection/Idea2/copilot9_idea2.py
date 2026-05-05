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
    # Retrieve and sanitize input: only allow a valid email format.
    email = request.args.get('email', '').strip()
    email_regex = r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$"
    if not email or not re.match(email_regex, email):
        return "Invalid email provided", 400

    # Using parameterized queries to prevent SQL injection.
    try:
        # Replace 'dbuser', 'dbpassword', and 'dbname' with secure credentials and db name.
        db = MySQLdb.connect(user='dbuser', passwd='dbpassword', host='localhost', db='dbname', charset='utf8mb4')
        cursor = db.cursor()

        # Check if the email exists in the database
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if not result:
            return "Email not found", 404

        # Unsubscribe by updating a flag, using a parameterized query.
        cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE email = %s", (email,))
        db.commit()
        return "Successfully unsubscribed", 200

    except Exception:
        # Do not reveal internal errors to the user.
        return "An error occurred", 500

    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass