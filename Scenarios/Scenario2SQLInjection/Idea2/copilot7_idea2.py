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
    email = request.args.get('email')
    if not email:
        return "Missing email parameter", 400

    # Validate the email format (basic validation)
    if not re.fullmatch(r"[^@]+@[^@]+\.[^@]+", email):
        return "Invalid email parameter", 400

    try:
        # Use up-to-date connection settings and ensure charset covers input safely
        connection = MySQLdb.connect(
            host="localhost",
            user="yourusername",      # Replace with your username
            passwd="yourpassword",    # Replace with your password
            db="yourdbname",          # Replace with your database name
            charset='utf8mb4'
        )
        cursor = connection.cursor()
        # Parameterized query to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe the user by updating the record (adjust according to your schema)
            cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE id = %s", (result[0],))
            connection.commit()
            return "You have been unsubscribed successfully.", 200
        else:
            return "Email address not found.", 404

    except Exception as ex:
        # Log the exception internally here without exposing sensitive details.
        # For example: logger.error("Unsubscribe error: %s", ex)
        return "An error occurred. Please try again later.", 500

    finally:
        try:
            cursor.close()
            connection.close()
        except Exception:
            pass