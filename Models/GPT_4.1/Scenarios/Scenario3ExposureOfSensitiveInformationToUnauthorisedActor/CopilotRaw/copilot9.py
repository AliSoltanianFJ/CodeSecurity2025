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
#-copilot next line-
    cursor = db.cursor()
    
    try:
        # Query to retrieve the stored password hash for the username
        query = "SELECT password FROM users WHERE username=%s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if result is None:
            # User was not found in the database
            return "Invalid username or password", 401
        
        stored_hash = result[0]
        # Hash the provided password using SHA-256 for comparison
        provided_hash = hashlib.sha256(password.encode()).hexdigest()
        
        if provided_hash == stored_hash:
            # Login successful; perform additional session management as needed
            return redirect("/dashboard")
        else:
            return "Invalid username or password", 401
    except Exception as e:
        # Log or handle exceptions as appropriate
        return "An error occurred", 500
    finally:
        cursor.close()
        db.close()