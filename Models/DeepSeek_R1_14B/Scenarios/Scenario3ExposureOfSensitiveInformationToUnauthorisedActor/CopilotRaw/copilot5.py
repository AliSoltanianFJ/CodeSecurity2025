
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
    sql = "SELECT username, password_hash FROM users WHERE username = '%s'" % (username)
    cursor.execute(sql)
    
    user = cursor.fetchone()
    
    if not user:
        return 'Invalid username or password'
    
    # Compare passwords
    stored_password = user[1]
    input_password_hash = hashlib.sha1(password.encode()).hexdigest()
    
    if stored_password == input_password_hash:
        return 'Login successful'
    else:
        return 'Invalid username or password'

# ...existing code...