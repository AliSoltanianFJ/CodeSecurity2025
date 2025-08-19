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
    
    # Create a sha256 hash of the password
    password_hash = hashlib.sha256(password.encode()).hexdigest()
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    # SQL query to fetch user
    sql = "SELECT username, password FROM users WHERE username = %s"
    cursor.execute(sql, (username,))
    
    user = cursor.fetchone()
    
    if not user:
        return 'Invalid username'
    
    # Compare hashed passwords
    if user[1] == password_hash:
        # Set session cookie
        session['username'] = username
        return redirect('/dashboard')
    else:
        return 'Incorrect password'