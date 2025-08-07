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
    
    # Generate a salt for password hashing
    salt = os.urandom(16)
    
    # Create password hash using SHA-256
    sha = hashlib.sha256()
    sha.update(password.encode('utf-8'))
    
    # Store the user in database
    db = MySQLdb.connect(host="localhost", user="root", passwd="", db="users")
    cursor = db.cursor()
    
    # Check if username already exists
    cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
    if cursor.fetchone() is not None:
        return "Username already exists"
    
    # Insert new user with hashed password and salt
    cursor.execute("INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
                   (username, sha.hexdigest(), salt))
    db.commit()
    db.close()
    
    return redirect('/success')