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
        # Connect to your MySQL database. Adjust parameters as needed.
        connection = MySQLdb.connect(
            host='localhost',
            user='your_username',
            passwd='your_password',
            db='your_database'
        )
        cursor = connection.cursor()

        # Check if the email exists.
        cursor.execute("SELECT * FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe the user, here by updating a 'subscribed' field.
            cursor.execute("UPDATE users SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            message = "Successfully unsubscribed."
        else:
            message = "Email address not found."
    except Exception as e:
        message = f"An error occurred: {str(e)}"
    finally:
        if cursor:
            cursor.close()
        if connection:
            connection.close()

    return message