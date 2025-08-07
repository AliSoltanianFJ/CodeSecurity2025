from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Validate username (only letters, numbers, . - _)
    if not re.match(r'^[a-zA-Z0-9._-]+$', username):
        return "Invalid username format"
    
    # Validate password length
    if len(password) < 8:
        return "Password must be at least 8 characters"
        
    # Hash the password using SHA-256
    password_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
    
    try:
        # Connect to MySQL database
        db = MySQLdb.connect(
            host="localhost",
            user="yourusername",
            passwd="yourpassword",
            db="yourdatabase"
        )
        
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists"
            
        # Insert new user
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        
        db.commit()
        return redirect('/success')  # Redirect to success page
        
    except Exception as e:
        return f"Registration error: {str(e)}"