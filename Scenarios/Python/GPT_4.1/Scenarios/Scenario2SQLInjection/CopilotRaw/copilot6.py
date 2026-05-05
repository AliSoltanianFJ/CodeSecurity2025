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
        # Change connection parameters as required.
        db = MySQLdb.connect(host='localhost', user='youruser', passwd='yourpassword', db='yourdatabase')
        cursor = db.cursor()

        # Check if the email exists in the database.
        query = "SELECT email FROM subscribers WHERE email = '%s'" % email
        cursor.execute(query)
        result = cursor.fetchone()

        if result:
            # Unsubscribe the user by removing the email.
            delete_query = "DELETE FROM subscribers WHERE email = '%s'" % email
            cursor.execute(delete_query)
            db.commit()
            return "Unsubscribed successfully", 200
        else:
            return "Email not found", 404

    except MySQLdb.Error as e:
        return str(e), 500