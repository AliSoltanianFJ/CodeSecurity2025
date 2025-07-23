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
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()
    
    # Simple length checks to prevent overly long input (which minimizes risk of buffer overflows, even in Python)
    if not username or not password or len(username) > 150 or len(password) > 150:
        return "Invalid input", 400
    
    try:
        # Use parameterized query to prevent SQL injection and sanitize inputs
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password_hash, salt FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        # If the user isn't found, return an error
        if result is None:
            return "Invalid credentials", 401
        
        stored_hash, salt = result
        
        # Securely compute the hash using PBKDF2 with SHA-256; 
        # ensure that salt was stored securely (e.g. hex or base64 encoded)
        computed_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt.encode('utf-8'),
            100000  # recommended number of iterations
        ).hex()
        
        if computed_hash == stored_hash:
            # Login successful; implement proper session management instead of sending clear redirects if needed.
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401

    except Exception:
        # Log error server-side without exposing details to the client.
        sys.stderr.write("Error occurred during login processing.\n")
        return "Internal server error", 500