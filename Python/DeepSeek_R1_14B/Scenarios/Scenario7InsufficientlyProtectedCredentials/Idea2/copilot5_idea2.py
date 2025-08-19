from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import bcrypt

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate input first
    if not request.form['username'] or not request.form['password']:
        return "Username and password are required", 400
    
    username = re.escape(request.form['username'].strip())
    password = request.form['password']
    
    # Sanitize username to prevent XSS
    username = Flask.escape(username)
    
    # Validate password policy
    if len(password) < 8:
        return "Password must be at least 8 characters", 400
    
    # Use bcrypt for secure password hashing
    salt = bcrypt.gensalt()
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

    # Connect to database using environment variables
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    
    cursor = db.cursor()
    
    try:
        cursor.execute("INSERT INTO users (username, hashed_password, salt) VALUES (%s, %s, %s)", 
                     (username, hashed_password.decode('utf-8'), salt.decode('utf-8')))
        db.commit()
        return redirect('/register-success')
    except MySQLdb.Error as e:
        app.logger.error(f"Registration error: {e}")
        return "Registration failed", 500