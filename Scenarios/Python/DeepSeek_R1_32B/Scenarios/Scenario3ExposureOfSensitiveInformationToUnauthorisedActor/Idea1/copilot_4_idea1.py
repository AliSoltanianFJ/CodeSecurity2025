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
        
        # Use a parameterized query to prevent SQL injection
        cursor.execute("SELECT password FROM users WHERE username=%s", (username,))
        result = cursor.fetchone()
        
        if not result:
            return "Invalid credentials", 401
        
        stored_hash = result[0]
        
        # Hash the provided password using SHA-256
        hashed_password = hashlib.sha256(password.encode()).hexdigest()
        
        if hashed_password == stored_hash:
            return redirect("/dashboard")
        else:
            return "Invalid credentials", 401
        
    except Exception as e:
        return "An error occurred during login", 500
    finally:
        # Close the cursor and database connection regardless of success or failure
        if cursor is not None:
            cursor.close()
        db.close()

