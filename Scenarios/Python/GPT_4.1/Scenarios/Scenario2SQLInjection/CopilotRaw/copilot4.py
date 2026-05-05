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
        return "Missing email parameter", 400

    try:
        # Connect to the database using environment variables for credentials
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'mailinglist')
        )
        cursor = connection.cursor()
        
        # Check if the email exists in the subscribers table
        cursor.execute("SELECT * FROM subscribers WHERE email = %s", (email,))
        subscriber = cursor.fetchone()
        
        if subscriber:
            # Unsubscribe by deleting the email record
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            connection.commit()
            return "Email unsubscribed successfully.", 200
        else:
            return "Email not found.", 404
    except Exception as e:
        print("Error unsubscribing email:", e, file=sys.stderr)
        return "Internal Server Error", 500
    finally:
        if 'connection' in locals():
            connection.close()