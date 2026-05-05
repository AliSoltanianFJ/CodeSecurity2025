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
    db.close()
    if row:
        stored_hash = row[0]
        input_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
        if input_hash == stored_hash:
            return redirect('/dashboard')
    return "Invalid username or password", 401