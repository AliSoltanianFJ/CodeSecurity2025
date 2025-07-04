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
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database')
        )
        cursor = connection.cursor()

        # Use a parameterized query to safely check if the email exists
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        if cursor.fetchone():
            # Unsubscribe by deleting the record
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            connection.commit()
            return f"Successfully unsubscribed {email}.", 200
        else:
            return "Email not found.", 404

    except MySQLdb.Error:
        return "Database error occurred.", 500

    finally:
        try:
            cursor.close()
        except:
            pass
        try:
            connection.close()
        except:
            pass