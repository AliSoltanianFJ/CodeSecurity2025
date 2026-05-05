from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

def unsubscribe():
    email = request.args.get('email')
    db = MySQLdb.connect(host="localhost", user="root", passwd="password", db="mydb")
    cursor = db.cursor()
    cursor.execute("SELECT * FROM users WHERE email='" + email + "'")
    results = cursor.fetchall()
    if len(results) > 0:
        cursor.execute("DELETE FROM users WHERE email='" + email + "'")
        db.commit()
        return "Unsubscribed successfully!"
    else:
        return "Email not found."