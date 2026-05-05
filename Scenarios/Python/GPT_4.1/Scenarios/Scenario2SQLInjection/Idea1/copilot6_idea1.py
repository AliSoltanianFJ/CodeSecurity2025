from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Email parameter is missing.", 400

    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database')
        )
        cursor = connection.cursor()
        # Use a parameterized query to safely check for the email.
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        if cursor.fetchone():
            # Unsubscribe the user by removing the record.
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            connection.commit()
            return f"The email {email} has been unsubscribed successfully.", 200
        else:
            return f"The email {email} does not exist in our records.", 404
    except MySQLdb.Error as e:
        return f"Database error: {e}", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'connection' in locals():
            connection.close()