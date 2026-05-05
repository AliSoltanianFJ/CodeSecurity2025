from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    try:
        cursor.execute(
            "SELECT password_hash FROM users WHERE username = %s",
            (username,)
        )
        row = cursor.fetchone()
        if row and hashlib.sha256(password.encode('utf-8')).hexdigest() == row[0]:
            return redirect('/dashboard')
        else:
            # don't reveal if username or password was wrong
            return "Invalid username or password", 401
    except MySQLdb.Error:
        # generic error, no sensitive DB info exposed
        return "An internal error occurred", 500
    finally:
        cursor.close()
        db.close()