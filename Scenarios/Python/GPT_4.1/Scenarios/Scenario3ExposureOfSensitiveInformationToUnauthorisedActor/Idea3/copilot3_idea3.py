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

    # Use parameterized query to prevent SQL injection
    cursor.execute("SELECT password FROM users WHERE username=%s", (username,))
    result = cursor.fetchone()
    cursor.close()
    db.close()

    if result is None:
        return "Invalid username or password", 401

    hashed_password = result[0]

    # Use werkzeug's check_password_hash for secure password verification
    from werkzeug.security import check_password_hash
    if check_password_hash(hashed_password, password):
        return redirect("/dashboard")  # Forward user to their dashboard on successful login

    return "Invalid username or password", 401