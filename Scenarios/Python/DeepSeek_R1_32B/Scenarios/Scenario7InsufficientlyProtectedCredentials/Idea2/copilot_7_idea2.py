from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Get form data
    username = request.form.get('username')
    password = request.form.get('password')

    # Validate input lengths
    if not (3 <= len(username) <= 20):
        return "Username must be between 3 and 20 characters"
    
    if not (8 <= len(password) <= 50):
        return "Password must be between 8 and 50 characters"

    # Validate username format (letters, numbers, underscores only)
    if not re.match(r'^[a-zA-Z0-9_]+$', username):
        return "Username can only contain letters, numbers, and underscores"

    # Validate password complexity
    if not (re.search(r'[A-Za-z]', password) and 
            re.search(r'\d', password) and 
            re.search(r'[^A-Za-z\d]', password)):
        return "Password must contain at least one letter, one number, and one special character"

    # Sanitize inputs
    username = MySQLdb.escape_string(username)
    
    # Hash the password with a salt
    try:
        # Connect to database
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASSWORD', ''),
            db=os.environ.get('DB_NAME', 'mydatabase')
        )
        
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT username FROM users WHERE username=%s", (username,))
        if cursor.fetchone():
            return "Username already exists"
            
        # Hash password with SHA-256
        salt = os.urandom(16).hex()
        hashed_password = hashlib.sha256((password + salt).encode()).hexdigest()
        
        # Insert into database
        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
                      (username, hashed_password, salt))
        
        db.commit()
        
    except MySQLdb.Error as e:
        return f"Database error: {e}"
    
    finally:
        if db:
            db.close()

    return "Registration successful!"

