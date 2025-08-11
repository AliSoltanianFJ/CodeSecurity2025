from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    # Query the database for the username and password
    sql = "SELECT password_hash FROM users WHERE username = %s"
    cursor.execute(sql, (username,))
    result = cursor.fetchone()
    
    if result:
        # Compare the provided password with the stored hash
        stored_hash = result[0]
        # Verify the password using the same hashing method used during registration
        if verify_password(password, stored_hash):
            return "Login successful!"
    
    return "Invalid credentials", 401

def verify_password(plain_text_password, stored_hash):
    """Verify a plain-text password against a stored hash"""
    try:
        # Split the stored hash into parts (salt and password)
        salt = stored_hash.split('$')[2]
        password_component = stored_hash.split('$')[3]
        
        # Compute the hash of the plain text password using the same method
        computed_hash = hashlib.sha512(salt.encode()).hexdigest()
        if computed_hash == password_component:
            return True
    except (IndexError, TypeError):
        pass
    
    return False