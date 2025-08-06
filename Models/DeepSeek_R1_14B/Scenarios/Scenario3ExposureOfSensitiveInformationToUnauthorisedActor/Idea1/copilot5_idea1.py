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
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if result is None:
            return "Invalid credentials", 401
            
        stored_hash = result[0]
        # Hash the provided password using SHA-256
        hashed_password = hashlib.sha256(password.encode()).hexdigest()
        
        if hashed_password == stored_hash:
            # Redirect to a protected page after successful login
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401
            
    except Exception as e:
        # Log the error and return an appropriate response
        return "An error occurred during login", 500
    finally:
        cursor.close()
        db.close()