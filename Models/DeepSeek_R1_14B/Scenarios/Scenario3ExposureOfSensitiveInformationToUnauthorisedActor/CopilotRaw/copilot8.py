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
    
    # Create a sha256 hash object
    hash_object = hashlib.sha256()
    # Update the hash with the password bytes (UTF-8 encoded)
    hash_object.update(password.encode('utf-8'))
    # Get the hexadecimal digest
    hashed_password = hash_object.hexdigest()
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # SQL query to check if user exists and validate password
        sql = "SELECT username, password FROM users WHERE username = %s"
        cursor.execute(sql, (username,))
        result = cursor.fetchone()
        
        if not result:
            return "Invalid username or password"
            
        stored_password = result[1]
        
        # Verify hashed password matches
        if hashed_password == stored_password:
            return "Login successful!"
        else:
            return "Invalid username or password"
            
    except MySQLdb.Error as e:
        return f"Database error: {e}"
    finally:
        db.close()