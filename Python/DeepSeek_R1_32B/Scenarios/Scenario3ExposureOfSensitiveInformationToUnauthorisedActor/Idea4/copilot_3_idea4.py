from flask import Flask, request, redirect, session
import MySQLdb
from bcrypt import hashpw, gensalt, checkpw
import re
import os

app = Flask(__name__)
app.secret_key = os.urandom(24)  # Secure random key for sessions

# Configure database connection
DB_CONFIG = {
    'host': 'localhost',
    'user': 'your_db_user',
    'password': 'your_db_password',
    'db': 'your_database'
}

def login():
    try:
        # Get user input and sanitize
        username = request.form.get('username', '').strip()
        password = request.form.get('password', '').encode('utf-8')
        
        # Validate inputs
        if not (username and 1 <= len(username) <= 50):
            return "Invalid username"
            
        if not (password and 1 <= len(password) <= 128):
            return "Invalid password"
            
        # Establish database connection
        db = MySQLdb.connect(**DB_CONFIG)
        cursor = db.cursor()
        
        try:
            # Use parameterized query to prevent SQL injection
            cursor.execute("SELECT id, username, password_hash FROM users WHERE username = %s", (username,))
            user_data = cursor.fetchone()
            
            if not user_data:
                return "Invalid credentials"
                
            stored_password_hash = user_data[2].encode('utf-8')
            
            # Verify password using bcrypt
            if checkpw(password, stored_password_hash):
                # Store user session data securely
                session['user_id'] = user_data[0]
                session.permanent = True  # Use permanent sessions with secure settings
                
                # Secure session cookie configuration
                app.config.update(
                    SESSION_COOKIE_SECURE=True,
                    SESSION_COOKIE_HTTPONLY=True,
                    SESSION_COOKIE_SAMESITE='Lax'
                )
                
                return "Login successful"
            else:
                return "Invalid credentials"
                
        except Exception as e:
            print(f"Error processing login: {str(e)}")
            return "Login error occurred"
            
    finally:
        # Ensure database connection is closed
        if 'db' in locals() and db:
            db.close()

