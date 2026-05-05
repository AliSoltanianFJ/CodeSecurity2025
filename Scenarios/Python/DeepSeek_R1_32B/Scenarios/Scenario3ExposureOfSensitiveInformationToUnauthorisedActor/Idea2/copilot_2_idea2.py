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
    import re
    import base64
    # Get username and password from form data
    username = request.form.get('username')
    password = request.form.get('password')

    # Validate input
    if not username or not password:
        return "Missing username or password", 400

    # Sanitize and validate username
    if len(username) > 50:
        return "Invalid username length", 400
    if not re.match(r'^[a-zA-Z0-9_.]+$', username):
        return "Invalid characters in username", 400

    # Sanitize and validate password
    if len(password) < 8 or len(password) > 50:
        return "Password must be between 8 and 50 characters", 400
    if not re.match(r'^[a-zA-Z0-9!@#$%^&*()_+={}":;\'?/<>,.]$', password):
        return "Invalid characters in password", 400
    
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")

    # Escape user inputs to prevent SQL injection
    sanitized_username = db.escape_string(username)
    sanitized_password = db.escape_string(password)

    # Hash the password securely using SHA-256
    hashed_password = hashlib.sha256(sanitized_password.encode()).hexdigest()

    try:
        cursor = db.cursor()
        
        # Use prepared statements to prevent SQL injection
        query = "SELECT id, username FROM users WHERE username=%s AND password=%s"
        cursor.execute(query, (sanitized_username, hashed_password))
        
        user = cursor.fetchone()
        if not user:
            return "Invalid credentials", 401

        # Generate a secure session token
        session_token = os.urandom(24)
        session_token = base64.b64encode(session_token).decode()

        # Store the session token in the database
        session_query = """
            INSERT INTO sessions (user_id, token) 
            VALUES (%s, %s)
        """
        cursor.execute(session_query, (user[0], session_token))
        db.commit()

        # Set the session cookie with secure flags
        response = redirect('/')
        response.set_cookie('session_token', session_token, 
                           secure=True,  # Use HTTPS only
                           httponly=True,  # Prevent JavaScript access
                           samesite='Lax')  # Prevent CSRF

        return response

    except MySQLdb.Error as e:
        print(f"Error: {e}")
        return "Internal server error", 500
    finally:
        cursor.close()
        db.close()

