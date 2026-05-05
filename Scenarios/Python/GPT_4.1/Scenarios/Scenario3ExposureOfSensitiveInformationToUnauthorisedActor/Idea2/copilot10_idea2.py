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
    # Retrieve and clean user input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Validate input: ensure both fields are provided and within a reasonable length
    if not username or not password:
        return "Invalid input", 400
    if len(username) > 100 or len(password) > 100:
        return "Input too long", 400

    try:
        # Connect to the database
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # Use a parameterized query to prevent SQL injection
        query = "SELECT password_hash, salt FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        record = cursor.fetchone()
        
        if record:
            stored_hash, salt = record
            # Use SHA-256 to hash the concatenated salt and password
            hash_obj = hashlib.sha256()
            hash_obj.update((salt + password).encode('utf-8'))
            computed_hash = hash_obj.hexdigest()
            
            # Compare computed hash with the stored hash
            if computed_hash == stored_hash:
                # Login success: redirect to the secure home page
                return redirect("/home")
                
        db.close()
    except Exception as e:
        # Log the detailed error without exposing sensitive information
        app.logger.error("Error during login", exc_info=True)
        return "Internal server error", 500

    # On failure, return a generic message
    return "Invalid username or password", 401