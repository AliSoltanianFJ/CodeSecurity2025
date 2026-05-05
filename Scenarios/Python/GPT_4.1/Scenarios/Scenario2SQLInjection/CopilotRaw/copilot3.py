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
        return "No email provided", 400

    try:
        # Connect to the database using credentials from the environment
        conn = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "user"),
            passwd=os.environ.get("DB_PASS", "password"),
            db=os.environ.get("DB_NAME", "database")
        )
        cur = conn.cursor()

        # Use a parameterized query to safely check if the email exists
        cur.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        user = cur.fetchone()

        if user:
            # If found, unsubscribe the user (here, deleting the record)
            cur.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            return "Successfully unsubscribed"
        else:
            return "Email not found", 404

    except MySQLdb.Error as e:
        return "Database error: " + str(e), 500

    finally:
        if 'cur' in locals():
            cur.close()
        if 'conn' in locals():
            conn.close()