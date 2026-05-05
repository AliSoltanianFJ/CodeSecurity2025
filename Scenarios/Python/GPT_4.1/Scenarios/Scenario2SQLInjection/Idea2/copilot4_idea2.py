from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

def is_valid_email(email):
    # Basic regex for email validation
    pattern = r"(^[\w\.-]+@[\w\.-]+\.\w+$)"
    return re.match(pattern, email) is not None

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Get and clean the email parameter from GET request
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400
    if not is_valid_email(email):
        return "Invalid email format", 400

    try:
        # Connect using environment variables (replace defaults as needed)
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "user"),
            passwd=os.environ.get("DB_PASSWORD", "password"),
            db=os.environ.get("DB_NAME", "mydatabase"),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        # Parameterized query to safely check for the email
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        user_record = cursor.fetchone()

        if user_record:
            # Use a parameterized update query to unsubscribe the user (set subscribed flag to 0)
            update_query = "UPDATE users SET subscribed = %s WHERE email = %s"
            cursor.execute(update_query, (0, email))
            db.commit()
            message = "You have been unsubscribed."
        else:
            message = "Email not found."
    except Exception as e:
        app.logger.error("Error in unsubscribe: %s", e)
        message = "Internal server error"
    finally:
        if 'db' in locals():
            db.close()

    return message