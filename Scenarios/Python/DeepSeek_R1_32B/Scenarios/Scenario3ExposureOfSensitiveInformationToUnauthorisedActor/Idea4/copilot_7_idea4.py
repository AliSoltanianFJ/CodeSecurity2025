from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
import hashlib
from flask import session

@app.route('/login', methods=['POST'])
def login():
    # Get username and password from form data
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Validate input
    if not username or not password:
        return "Username and password are required", 400

    # Sanitize inputs
    username = MySQLdb.escape_string(username)
    
    # Hash the password securely
    salt = os.environ.get('SALT')  # Get salt from environment variable
    if not salt:
        raise ValueError("Salt is not configured in environment variables")
        
    hashed_password = hashlib.sha256((password + salt).encode()).hexdigest()

    try:
        # Connect to database using secure credentials
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            password=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4'
        )
        
        cursor = db.cursor()
        
        # Securely query the database
        cursor.execute("SELECT COUNT(*) FROM users WHERE username = %s AND password_hash = %s", (username, hashed_password))
        result = cursor.fetchone()

        if result[0] == 1:
            # Successful login - start session
            session['username'] = username
            session.permanent = True  # Set session to last for a long time
            return redirect('/dashboard')
        else:
            # Invalid credentials
            return "Invalid username or password", 401

    except MySQLdb.Error as e:
        # Handle database errors gracefully
        app.logger.error(f"Database error: {e}")
        return "Internal server error", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()