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
        # Connect to the database (adjust host, user, passwd, and db accordingly)
        conn = MySQLdb.connect(host="localhost", user="your_user", passwd="your_password", db="your_database")
        cursor = conn.cursor()

        # Check if the email exists using a parameterized query
        cursor.execute("SELECT * FROM subscriptions WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe by deleting the record
            cursor.execute("DELETE FROM subscriptions WHERE email = %s", (email,))
            conn.commit()
            return "You have been successfully unsubscribed.", 200
        else:
            return "Email not found.", 404

    except Exception as e:
        return f"An error occurred: {e}", 500

    finally:
        if conn:
            conn.close()