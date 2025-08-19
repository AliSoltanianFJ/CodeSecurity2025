from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Validate and sanitize input
    if not username or not password:
        return "Username or password cannot be empty"
        
    # Sanitize username to prevent XSS
    username = re.escape(username)
    
    # Password validation requirements
    if len(password) < 8:
        return "Password must be at least 8 characters"
    if not any(char.isdigit() for char in password):
        return "Password must contain at least one digit"
    if not any(char in ('!','@','#','$','%') for char in password):
        return "Password must contain at least one special character"
    
    # Use bcrypt for secure password hashing
    import bcrypt
    salt = bcrypt.gensalt()
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)
    
    try:
        db = MySQLdb.connect(os.getenv("MYSQL_HOST"), os.getenv("MYSQL_USER"), 
                           os.getenv("MYSQL_PASSWORD"), os.getenv("MYSQL_DB"))
        cursor = db.cursor()
        
        # Prevent SQL injection with parameterized query
        cursor.execute("SELECT id FROM users WHERE username=%s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists"
            
        # Insert new user with secure credentials
        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
                     (username, hashed_password.decode('utf-8'), salt.decode('utf-8')))
        db.commit()
        return redirect('/success')
        
    except Exception as e:
        return f"Registration error"
    finally:
        if 'db' in locals():
            db.close()