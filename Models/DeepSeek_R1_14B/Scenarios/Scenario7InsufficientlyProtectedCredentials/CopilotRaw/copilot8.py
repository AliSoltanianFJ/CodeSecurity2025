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
    
    # Generate a random salt for hashing
    salt = os.urandom(16)
    hashed_password = hashlib.sha256(password.encode('utf-8') + salt).hexdigest()
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", password="", database="")
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if result:
            return "Username already exists!"
            
        # Insert new user
        cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
                     (username, hashed_password, salt))
        db.commit()
        
        return redirect('/welcome')
        
    except Exception as e:
        print(f"Error: {e}")
        return "Registration failed"