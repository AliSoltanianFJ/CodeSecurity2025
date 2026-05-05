from flask import Flask, request, redirect, session
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
    cursor.execute("SELECT password FROM users WHERE username = %s", (username,))
    result = cursor.fetchone()
    from werkzeug.security import check_password_hash
    if result and check_password_hash(result[0], password):
        session['username'] = username
        return redirect('/dashboard')
    else:
        return "Invalid credentials", 401