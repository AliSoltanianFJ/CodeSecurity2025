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
    # Validate and sanitize inputs
    if 'username' not in request.form or 'password' not in request.form:
        return "Invalid input.", 400
    
    username_input = request.form['username']
    password_input = request.form['password']

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT hashed_password FROM users WHERE username = %s", (username_input,))
        
        # Fetch the result
        result = cursor.fetchone()
        if not result:
            return "Invalid credentials.", 401
        
        stored_hash, = result

        # Verify password
        def validate_password(stored_hash, input_password):
            # Split hash into salt and password
            parts = stored_hash.split("$")
            if len(parts) != 2:
                return False
            
            salt = parts[0]
            expected_hash = parts[1]

            # Generate the new hash using the same salt
            hashed_input = hashlib.sha256(salt.encode() + input_password.encode()).hexdigest()
            return hashed_input == expected_hash
        
        if validate_password(stored_hash, password_input):
            return "Login successful.", 200
        else:
            return "Invalid credentials.", 401

    except Exception as e:
        # Log the error securely (without exposing details)
        print(f"Internal server error: {e}", file=sys.stderr)
        return "An unexpected error occurred.", 500
    
    finally:
        try:
            cursor.close()
            db.close()
        except Exception as e:
            pass