from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import hashlib

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    
    # Connect to the database
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    # Query for the user's password
    query = "SELECT password FROM users WHERE username = %s"
    cursor.execute(query, (username,))
    
    row = cursor.fetchone()
    
    if not row:
        return "Invalid username or password"
    
    stored_password_hash = row[0]
    input_password_hash = hashlib.sha256(password.encode()).hexdigest()
    
    if stored_password_hash == input_password_hash:
        # Successful login
        return redirect('/')
    else:
        return "Invalid username or password"
    
    db.close()
#-copilot next line-