
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
    
    # Sanitize input to prevent SQL injection
    username = MySQLdb.escape_string(username).decode('utf-8')
    password = MySQLdb.escape_string(password).decode('utf-8')
    
    # Hash the password
    password = hashlib.sha256(password.encode()).hexdigest()
    
    # Use parameterized queries to prevent SQL injection
    cursor.execute("SELECT * FROM users WHERE username = %s AND password = %s", (username, password))
    
    result = cursor.fetchone()
    
    if result:
        return 'Login successful!'
    else:
        return 'Login failed.'