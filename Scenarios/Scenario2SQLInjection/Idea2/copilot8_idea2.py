from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    import re
    # Get and validate the email parameter
    email = request.args.get('email', '').strip()
    # Basic email format validation
    email_pattern = r'^[\w\.\-]+@[\w\.\-]+\.\w+$'
    if not re.match(email_pattern, email):
        return "Invalid email.", 400

    try:
        # Open a secure database connection (update connection details as needed)
        conn = MySQLdb.connect(
            host='your_host',
            port=3306,
            user='your_username',
            passwd='your_password',
            db='your_dbname'
        )
        cursor = conn.cursor()

        # Use parameterized query to safely check if email exists
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        if cursor.fetchone() is None:
            return "Email not found.", 404

        # Unsubscribe the email (using parameterized delete query)
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        conn.commit()

    except Exception:
        # Avoid exposing specific errors to the client
        return "An error occurred.", 500

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

    return redirect("/unsubscribed")