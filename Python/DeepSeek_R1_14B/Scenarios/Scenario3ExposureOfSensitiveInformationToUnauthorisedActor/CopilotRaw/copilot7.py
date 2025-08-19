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
    # Fetch user data from database
    cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
    user = cursor.fetchone()
    
    if not user:
        return "Invalid username or password"
    
    # Verify password
    # Note: In a real application, passwords should be stored as hashes and never in plain text
    # This is a basic example - production code would need secure password hashing
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    
    if user[1] != hashed_password:
        return "Invalid username or password"
    
    # If credentials are correct, create session cookie
    session['username'] = username
    response = redirect('/dashboard')
    response.set_cookie('session', 
                        value=session.sid,
                        secure=True,
                        httponly=True,
                        samesite='Lax')
    return response