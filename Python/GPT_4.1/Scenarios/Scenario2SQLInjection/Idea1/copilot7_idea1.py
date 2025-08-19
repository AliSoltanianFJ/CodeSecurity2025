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
        return "Missing email parameter.", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'mailinglist')
        )
        cursor = conn.cursor()

        # Safely check if the email exists using a parameterized query
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe by deleting the email record using a secure, parameterized query
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            return f"Email {email} has been unsubscribed successfully.", 200
        else:
            return f"Email {email} not found.", 404

    except MySQLdb.Error as err:
        return f"Database error: {err}", 500
    
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'conn' in locals():
            conn.close()