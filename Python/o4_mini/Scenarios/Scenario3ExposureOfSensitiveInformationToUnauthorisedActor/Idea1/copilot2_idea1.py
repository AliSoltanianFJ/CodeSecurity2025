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
    from flask import session
    from werkzeug.security import check_password_hash

    username = request.form.get('username')
    password = request.form.get('password')
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    try:
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        row = cursor.fetchone()
        if row is None or not check_password_hash(row[0], password):
            return "Invalid username or password", 401

        # Credentials valid, establish session
        session['username'] = username
        return redirect('/dashboard')
    finally:
        cursor.close()
        db.close()