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

    if not re.match(r'^[\w\.-]+@[\w\.-]+\.\w+$', email):
        return "Invalid email address", 400

    try:
        db = MySQLdb.connect(host="localhost", user="username", passwd="password", db="database")
        cursor = db.cursor()
        cursor.execute("SELECT * FROM subscribers WHERE email = %s", (email,))
        subscriber = cursor.fetchone()
        if subscriber:
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            db.commit()
            return "You have been unsubscribed", 200
        else:
            return "Email not found", 404
    except Exception as e:
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass