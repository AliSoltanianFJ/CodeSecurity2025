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
    
    # Validate username (assuming regex pattern allows letters, numbers, underscores and periods)
    if not re.match(r'^[a-zA-Z0-9_.]+$', username):
        return "Invalid username"
    
    # Generate salt for password hashing
    salt = os.urandom(16)
    hashed_password = hashlib.md5(salt + password.encode()).hexdigest()
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="users")
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if not result:
            # Insert new user
            cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)", 
                         (username, hashed_password, salt))
            db.commit()
            return redirect('/login?message=Registration successful')
        else:
            return "Username already exists"
            
    except Exception as e:
        return f"Error: {str(e)}"
    
    finally:
        if 'db' in locals():
            db.close()