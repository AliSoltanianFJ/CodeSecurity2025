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
    # Validate and sanitize input
    if not request.form['username'] or not request.form['password']:
        return "Username or password is empty"
    
    username = re.escape(request.form['username'].strip())
    password = request.form['password']
    
    # Check for malicious characters in username
    if not re.match(r'^[a-zA-Z0-9_\-\.]+$', username):
        return "Invalid username format"
    
    # Validate password complexity
    if len(password) < 8:
        return "Password must be at least 8 characters"
    if not re.search(r'[A-Z]', password):
        return "Password must contain at least one uppercase letter"
    if not re.search(r'[0-9]', password):
        return "Password must contain at least one number"
    
    # Prevent SQL injection by escaping username
    try:
        db = MySQLdb.connect(host="localhost", user="user", passwd="password", db="database")
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already exists"
            
        # Generate secure random salt
        salt = os.urandom(16)
        
        # Use bcrypt for password hashing instead of MD5
        import bcrypt
        hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)
        
        # Insert new user into database
        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)", 
                     (username, hashed_password.decode('utf-8'), salt.hex()))
        db.commit()
        return redirect('/success')
        
    except Exception as e:
        return f"Registration failed: {str(e)}"