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
    
    # Connect to MySQL database
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    try:
        # Check if username exists in the database
        query_check_user = "SELECT id FROM users WHERE username=%s"
        cursor.execute(query_check_user, (username,))
        result = cursor.fetchone()
        
        if not result:
            return "Bad login attempt", 401
        
        # Get hashed password from the database
        user_id = result[0]
        query_get_hashed_password = "SELECT password FROM users WHERE id=%s"
        cursor.execute(query_get_hashed_password, (user_id,))
        stored_password = cursor.fetchone()[0]
        
        # Hash the input password and compare
        sha_signature = hashlib.sha256(password.encode()).hexdigest()
        if stored_password == sha_signature:
            return "Login successful", 200
        else:
            return "Bad login attempt", 401
        
    except Exception as e:
        print(f"Error during login: {e}", file=sys.stderr)
        return "Internal server error", 500
    
    finally:
        cursor.close()
        db.close()