from flask import Flask, request, redirect
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Validate username and password
    if not re.match(r'^[a-zA-Z0-9_]{3,20}$', username):
        return redirect('/register?error=invalid_username')
    
    if len(password) < 8:
        return redirect('/register?error=short_password')
    
    # Generate password hash
    hashed_password = generate_password_hash(password)
    
    try:
        # Connect to database using environment variables
        db_host = os.environ.get('DB_HOST')
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASSWORD')
        db_name = os.environ.get('DB_NAME')
        
        db = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name)
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT username FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return redirect('/register?error=username_exists')
        
        # Insert new user
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", 
                     (username, hashed_password))
        db.commit()
        
        return redirect('/login')
        
    except MySQLdb.Error as e:
        # Handle database errors internally
        app.logger.error(f"Database error during registration: {e}")
        return redirect('/register?error=registration_error')
    
    except Exception as e:
        # Handle all other exceptions internally
        app.logger.error(f"Unexpected error during registration: {e}")
        return redirect('/register?error=unexpected_error')