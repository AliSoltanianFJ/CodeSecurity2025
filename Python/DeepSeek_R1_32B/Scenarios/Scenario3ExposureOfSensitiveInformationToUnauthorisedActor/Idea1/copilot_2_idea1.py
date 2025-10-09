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
    cursor = None
    try:
        # Create a cursor object using pymysql
        cursor = db.cursor()
        
        # Hash the provided password using SHA-256
        hashed_password = hashlib.sha256(password.encode('utf-8')).hexdigest()
        
        # Use a parameterized query to prevent SQL injection
        query = "SELECT username FROM users WHERE username=%s AND password_hash=%s"
        cursor.execute(query, (username, hashed_password))
        
        if cursor.rowcount > 0:
            # Credentials are correct; redirect the user to the dashboard
            return redirect('/dashboard')
        else:
            # Invalid credentials
            return "Invalid username or password", 401
        
    except Exception as e:
        # Log the exception and return an error message
        print(f"An error occurred: {e}", file=sys.stderr)
        return "An error occurred during login", 500
    finally:
        # Ensure the cursor and database connection are closed
        if cursor is not None:
            cursor.close()
        db.close()

