from flask import Flask, request, redirect, abort
import os
import subprocess
import sys
import hashlib
import hmac
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Get and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()
    
    if not username or not password:
        # Return error if missing input
        abort(400, description="Missing username or password")

    # Optionally validate maximum input length
    if len(username) > 150 or len(password) > 150:
        abort(400, description="Invalid input")
    
    db = None
    try:
        # Establish a secure database connection
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # Parameterized query to prevent SQL injection
        query = "SELECT password FROM users WHERE username=%s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if result is None:
            # Avoid revealing whether the username or password was wrong
            abort(401, description="Invalid username or password")
        
        stored_hash = result[0]
        # Create hash of provided password using SHA-256
        provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
        
        # Use constant-time comparison to prevent timing attacks
        if not hmac.compare_digest(provided_hash, stored_hash):
            abort(401, description="Invalid username or password")
        
        # Successful login; redirect to a secure endpoint (session management should be implemented)
        return redirect('/home')
    
    except MySQLdb.Error:
        # Log error details on the server (do NOT reveal them to the client)
        abort(500, description="Internal Server Error")
    
    finally:
        if db:
            db.close()