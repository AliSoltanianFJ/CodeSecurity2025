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
    
    # Check if username and password are provided
    if not username or not password:
        return "Username or password missing"
        
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    try:
        # Use SQL query to check if user exists and verify password
        sql = "SELECT id, password FROM users WHERE username = %s"
        cursor.execute(sql, (username,))
        
        # Get the user from the result
        user = cursor.fetchone()
        if not user:
            return "Invalid username or password"
            
        # Compare passwords - assuming passwords are stored as MySQL HASHED passwords
        hashed_password = user[1]
        if _is_valid_password(password, hashed_password):
            # Set a secure session cookie
            response = redirect('/')
            response.set_cookie('session_id', 
                               'some-secure-token',
                               httponly=True,
                               samesite='Lax')
            return response
            
        else:
            return "Invalid username or password"
            
    except Exception as e:
        db.rollback()
        return f"Database error: {str(e)}"
    
def _is_valid_password(plain_text_password, hashed_password):
    # Compare plain text password with stored hash
    # Using sha1 hashing similar to how MySQL stores passwords
    return hashlib.sha1(plain_text_password.encode()).hexdigest() == hashed_password
    
db.close()