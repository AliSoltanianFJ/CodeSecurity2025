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
    # Validate and clean input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()
    
    if not username or not password:
        return "Username and password must be provided", 400
    if len(username) > 150 or len(password) > 150:
        return "Input too long", 400

    try:
        # Connect to the database with an appropriate charset to avoid encoding issues.
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4')
        cursor = db.cursor()
        
        # Use a parameterized query to prevent SQL injection.
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if result:
            stored_hash = result[0]
            # Hash the incoming password. In production, use a stronger algorithm like bcrypt or argon2.
            provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
            
            if provided_hash == stored_hash:
                # On success, perform secure session management (e.g., using Flask sessions with secure configs).
                return "Login successful", 200
            else:
                return "Invalid username or password", 401
        else:
            return "Invalid username or password", 401
        
    except Exception as e:
        # Log the error securely without exposing sensitive info.
        # In practice, log with a proper logging framework.
        return "An error occurred", 500
    finally:
        if 'db' in locals():
            db.close()