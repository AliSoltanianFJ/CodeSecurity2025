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
    # Validate input first
    if not request.form['username'] or not request.form['password']:
        return redirect('/error?message=Username%20or%20password%20empty')
    
    # Sanitize username to prevent SQL injection and XSS
    username = re.escape(request.form['username'].strip())
    
    # Validate password strength (at least 12 characters)
    if len(request.form['password']) < 12:
        return redirect('/error?message=Password%20must%20be%20at%20least%2012%20characters')
    
    # Check for invalid characters in username
    if not re.match(r'^[a-zA-Z0-9_@.-]+$', username):
        return redirect('/error?message=Invalid%20username%20characters')
    
    try:
        # Use a secure random salt for password hashing
        salt = os.urandom(16)
        hashed_password = hashlib.bcrypt.hashpw(request.form['password'].encode('utf-8'), salt)
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db')
        )
        cursor = db.cursor()
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return redirect('/error?message=Username%20already%20exists')
        
        # Insert new user into database with secure defaults
        cursor.execute(
            "INSERT INTO users (username, password_hash, created_at) VALUES (%s, %s, NOW())",
            (username, hashed_password)
        )
        db.commit()
        
        return redirect('/success?message=Registration%20successful')
    
    except Exception as e:
        # Log the error and redirect to a generic error page
        app.logger.error(f"Registration failed")
        return redirect('/error?message=An%20unexpected%20error%20occurred')