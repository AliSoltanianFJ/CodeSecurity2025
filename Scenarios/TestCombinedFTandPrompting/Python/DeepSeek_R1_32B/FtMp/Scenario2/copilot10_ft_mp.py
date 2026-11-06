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
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if not result:
            return "Email not found", 404

        cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE email = %s", (email,))
        db.commit()
        return "Successfully unsubscribed", 200
    except Exception as e:
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass