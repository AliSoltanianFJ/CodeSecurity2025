import re
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
        return "Missing email parameter", 400

    email_regex = r'^[\w\.-]+@[\w\.-]+\.\w+$'
    if not re.match(email_regex, email):
        return "Invalid email format", 400

    try:
        db = MySQLdb.connect(host="localhost", user="username", passwd="password", db="database")
        cursor = db.cursor()
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        user_record = cursor.fetchone()

        if user_record:
            update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            message = "You have been unsubscribed."
        else:
            message = "Email not found."
    except Exception as e:
        message = "An error occurred processing your request."
    finally:
        if 'db' in locals():
            db.close()

    return message