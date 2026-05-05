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
    
    try:
        # Use a parameterized query to prevent SQL injection
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if result is None:
            return "Invalid credentials", 401
        
        stored_hash = result[0]
        # Hash the provided password using SHA-256
        hashed_password = hashlib.sha256(password.encode('utf-8')).hexdigest()
        
        if hashed_password == stored_hash:
            return redirect("/dashboard")
        else:
            return "Invalid credentials", 401
    except Exception as e:
        # Log the exception for debugging purposes
        print(f"An error occurred: {e}", file=sys.stderr)
        return "An error occurred during login", 500
    finally:
        cursor.close()
        db.close()

