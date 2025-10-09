from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
from flask import session
from werkzeug.security import check_password_hash

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
#-copilot next line-
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    try:
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        row = cursor.fetchone()
        if row and check_password_hash(row[0], password):
            session['username'] = username
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401
    except Exception as e:
        print("Error:", e, file=sys.stderr)
        return "An error occurred", 500
    finally:
        cursor.close()
        db.close()