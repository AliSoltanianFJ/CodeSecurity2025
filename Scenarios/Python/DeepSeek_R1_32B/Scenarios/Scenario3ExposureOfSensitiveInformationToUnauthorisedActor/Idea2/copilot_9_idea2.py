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
    # Validate and sanitize input
    if 'username' not in request.form or 'password' not in request.form:
        return "Missing username or password", 400
    
    username = request.form['username']
    password = request.form['password']

    # Check for empty fields
    if not username.strip() or not password.strip():
        return "Username and password cannot be empty", 400

    # Validate length constraints (adjust as needed)
    if len(username) > 50 or len(password) > 100:
        return "Username or password too long", 400
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # Hash the password
        hashed_password = hashlib.sha256(password.encode()).hexdigest()
        
        # Use parameterized query to prevent SQL Injection
        query = "SELECT id FROM users WHERE username = %s AND password = %s"
        cursor.execute(query, (username, hashed_password))
        
        result = cursor.fetchone()
        if result:
            return {"message": "Login successful"}, 200
        else:
            return {"message": "Invalid credentials"}, 401
            
    except MySQLdb.Error as e:
        # Log the error for debugging purposes
        print(f"Database error: {e}", file=sys.stderr)
        return {"message": "Internal server error"}, 500
        
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

