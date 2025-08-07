from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    from flask import session
    username = request.form['username']
    password = request.form['password']
    
    # Generate a salt for password hashing
    pass_salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac('sha256', password.encode(), pass_salt, 100000)
    
    try:
        # Check if user already exists
        cursor = app.db.cursor()
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if result:
            return "Username already exists"
            
        # Add new user
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password.hexdigest(), pass_salt.hex())
        )
        app.db.commit()
        
        # Set a flash message for success
        session['message'] = 'Registration successful! Please login.'
        return redirect('/login')
        
    except Exception as e:
        app.db.rollback()
        return f"Error: Registration failure"