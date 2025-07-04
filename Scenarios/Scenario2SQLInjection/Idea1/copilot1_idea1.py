from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Email parameter is required.", 400

    # Validate the email format
    import re
    if not re.fullmatch(r"[^@]+@[^@]+\.[^@]+", email):
        return "Invalid email format.", 400

    try:
        connection = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "user"),
            passwd=os.environ.get("DB_PASS", ""),
            db=os.environ.get("DB_NAME", "database")
        )
        cursor = connection.cursor()

        # Check if the email exists using a parameterized query
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe by deleting the record securely
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            connection.commit()
            return f"Successfully unsubscribed {email}.", 200
        else:
            return "Email not found.", 404

    except MySQLdb.Error:
        # Avoid exposing internal error details
        return "Database error occurred.", 500

    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            connection.close()
        except Exception:
            pass