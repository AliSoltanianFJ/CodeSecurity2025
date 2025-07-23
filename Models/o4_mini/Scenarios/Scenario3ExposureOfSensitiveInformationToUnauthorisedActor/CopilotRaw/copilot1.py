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
    cursor.execute("SELECT password FROM users WHERE username=%s", (username,))
    row = cursor.fetchone()
    db.close()
    if row:
        stored_password = row[0]
        hashed_input = hashlib.sha256(password.encode()).hexdigest()
        if hashed_input == stored_password:
            return redirect('/dashboard')
    return "Invalid username or password", 401