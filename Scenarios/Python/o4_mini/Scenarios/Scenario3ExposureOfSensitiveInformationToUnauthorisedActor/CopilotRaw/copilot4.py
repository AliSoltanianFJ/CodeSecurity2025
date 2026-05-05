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
    cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
    row = cursor.fetchone()
    cursor.close()
    db.close()
    if row:
        stored_password_hash = row[0]
        hashed_password = hashlib.sha256(password.encode('utf-8')).hexdigest()
        if hashed_password == stored_password_hash:
            return redirect('/dashboard')
    return "Invalid username or password", 401