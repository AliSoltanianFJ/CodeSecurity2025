from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Get 'email' from query parameters
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400

    # Validate email format
    import re
    email_regex = r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    if not re.match(email_regex, email):
        return "Invalid email format", 400

    try:
        # Establish database connection (replace connection details as needed)
        db = MySQLdb.connect(
            host="localhost",
            user="your_username",
            passwd="your_password",
            db="your_database",
            charset="utf8mb4"
        )
        cursor = db.cursor()

        # Check if the email exists
        select_query = "SELECT email FROM subscribers WHERE email = %s"
        cursor.execute(select_query, (email,))
        result = cursor.fetchone()

        if not result:
            return "Email not found", 404

        # Update the subscription status (assuming there is an 'unsubscribed' column)
        update_query = "UPDATE subscribers SET unsubscribed = 1 WHERE email = %s"
        cursor.execute(update_query, (email,))
        db.commit()

        return "You have been unsubscribed", 200

    except Exception as e:
        app.logger.error(f"Error during unsubscribe: {e}")
        return "Internal server error", 500

    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()