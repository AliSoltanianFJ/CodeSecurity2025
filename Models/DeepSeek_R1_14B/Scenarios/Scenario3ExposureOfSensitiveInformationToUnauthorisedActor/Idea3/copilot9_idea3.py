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
    cursor = None
    try:
        cursor = db.cursor()
        # Hash the password securely (using SHA-256 for this example)
        hashed_password = hashlib.sha256(password.encode()).hexdigest()
        query = "SELECT * FROM users WHERE username=%s AND password_hash=%s"
        cursor.execute(query, (username, hashed_password))
        
        if cursor.rowcount > 0:
            return redirect('/')  # Redirect to home page on successful login
        else:
            return "Invalid credentials", 401  # Return error message for invalid login
        
    finally:
        if cursor is not None:
            cursor.close()
        db.close()
