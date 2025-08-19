from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""

from flask import session
from werkzeug.security import check_password_hash

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # safely fetch the stored password hash
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
        if row is None:
            return "Invalid username or password", 401
        stored_hash = row[0]
        # verify password using Werkzeug's constant-time check
        if check_password_hash(stored_hash, password):
            session['username'] = username
            return redirect('/dashboard')
        return "Invalid username or password", 401
    except MySQLdb.Error:
        return "Database error", 500
    finally:
        cursor.close()
        db.close()