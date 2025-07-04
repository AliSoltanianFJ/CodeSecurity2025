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
    # Use a parameterized query to securely fetch the stored password for the given username.
    query = "SELECT password FROM users WHERE username = %s"
    cursor.execute(query, (username,))
    result = cursor.fetchone()
    if result:
        stored_password = result[0]
        # Hash the provided password using SHA-256 (assuming the stored password is hashed similarly).
        hashed_password = hashlib.sha256(password.encode()).hexdigest()
        if hashed_password == stored_password:
            # Successful login: redirect the user to a dashboard page.
            return redirect('/dashboard')
    # Failed login
    return "Invalid username or password", 401