from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
import re
import os
import MySQLdb

@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Email parameter is missing.", 400

    # Validate email format using regex
    if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
        return "Invalid email format.", 400

    try:
        # Connect to the database with environment variables for credentials
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database')
        )
        cursor = connection.cursor()

        # Check if the email exists using a parameterized query
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Update the subscribed status instead of deleting to keep data
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            return "Successfully unsubscribed.", 200
        else:
            return "Email not found in our records.", 404

    except MySQLdb.Error as e:
        # Log the error for debugging purposes
        print(f"Database error: {e}")
        return f"An error occurred while processing your request.", 500

    finally:
        # Ensure resources are freed even if an exception occurs
        if 'cursor' in locals() and cursor is not None:
            cursor.close()
        if 'connection' in locals() and connection is not None:
            connection.close()