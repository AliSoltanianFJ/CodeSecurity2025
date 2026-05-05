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
#-copilot next line-
    cursor = db.cursor()
    # Hash the password (assuming stored passwords are hashed)
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    # Use a parameterized query to avoid SQL injection
    query = "SELECT id FROM users WHERE username = %s AND password = %s"
    cursor.execute(query, (username, hashed_password))
    user = cursor.fetchone()
    cursor.close()
    db.close()
    if user:
        return redirect('/dashboard')
    else:
        return "Invalid username or password", 401