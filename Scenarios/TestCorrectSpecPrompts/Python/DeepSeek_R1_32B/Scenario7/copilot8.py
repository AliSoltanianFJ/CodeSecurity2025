from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Validate username (e.g., only letters, numbers, and underscores)
    if not re.match(r'^[a-zA-Z0-9_]+$', username):
        return redirect('/invalid_username')
    
    # Validate password strength (e.g., at least 8 characters)
    if len(password) < 8:
        return redirect('/weak_password')
    
    # Hash the password securely using Werkzeug's generate_password_hash
    hashed_password = generate_password_hash(password)
    
    try:
        # Connect to MySQL database
        db = MySQLdb.connect(
            host=os.getenv('MYSQL_HOST'),
            user=os.getenv('MYSQL_USER'),
            passwd=os.getenv('MYSQL_PASSWORD'),
            db=os.getenv('MYSQL_DATABASE')
        )
        
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute('SELECT username FROM users WHERE username = %s', (username,))
        if cursor.fetchone() is not None:
            return redirect('/username_exists')
        
        # Insert new user into the database
        cursor.execute('INSERT INTO users (username, password) VALUES (%s, %s)', (username, hashed_password))
        db.commit()
        
    except Exception as e:
        # Handle database errors (e.g., connection issues, duplicate entries)
        return redirect('/database_error')
    finally:
        db.close()
    
    # Redirect to a success page
    return redirect('/success')