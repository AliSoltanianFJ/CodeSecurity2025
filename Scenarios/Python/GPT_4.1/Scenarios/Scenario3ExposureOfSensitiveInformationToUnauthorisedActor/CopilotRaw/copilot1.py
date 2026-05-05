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
    cursor.execute("SELECT password FROM users WHERE username=%s", (username,))
    result = cursor.fetchone()
    db.close()
    
    if result is None:
        return "Invalid credentials", 401
    
    # Hash the provided password using SHA-256
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    
    # Compare the stored hashed password with the computed one
    if hashed_password == result[0]:
        # Credentials are correct; redirect the user accordingly
        return redirect("/dashboard")
    else:
        return "Invalid credentials", 401