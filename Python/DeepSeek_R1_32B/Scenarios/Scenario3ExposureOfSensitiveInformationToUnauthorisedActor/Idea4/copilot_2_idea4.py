from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb
import bcrypt

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Get username and password from form data
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return "Username and password are required", 400

    try:
        # Connect to database
        db = MySQLdb.connect(
            host=os.getenv("DB_HOST", "localhost"),
            user=os.getenv("DB_USER", "root"),
            passwd=os.getenv("DB_PASSWORD", ""), 
            db=os.getenv("DB_NAME", "mydatabase")
        )
        
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        query = """
            SELECT id, password_hash FROM users 
            WHERE username = %s
        """
        
        cursor.execute(query, (username,))
        user_data = cursor.fetchone()
        
        if not user_data:
            return "Invalid credentials", 401
            
        # Validate password hash securely
        user_id, stored_hash = user_data
        
        # Using bcrypt for secure password verification
        # Note: Replace with your actual hashing method
        if not bcrypt.checkpw(password.encode(), stored_hash):
            return "Invalid credentials", 401

        # Store user session data
        session['user_id'] = user_id
        session.permanent = True
        
        db.close()
        
        return redirect('/dashboard'), 200

    except Exception as e:
        app.logger.error(f"Login error: {str(e)}")
        return "An error occurred during login", 500

