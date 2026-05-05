from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
import re

def validate_input(user_input):
    # Only allow alphanumeric characters and limited special characters
    if re.match('^[a-zA-Z0-9_]+$', user_input):
        return True
    return False

@app.route('/login', methods=['POST'])
def login():
    try:
        username = request.form['username']
        password = request.form['password']

        # Validate input to prevent SQL injection and XSS
        if not validate_input(username) or not validate_input(password):
            return "Invalid characters in input", 400

        # Sanitize inputs by stripping whitespace
        username = username.strip()
        password = password.strip()

        # Create hashed password
        password_hash = hashlib.sha256(password.encode()).hexdigest()

        # Connect to database
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # SQL injection-safe query using prepared statements
        sql = "SELECT * FROM users WHERE username = %s AND password = %s"
        cursor.execute(sql, (username, password_hash))

        result = cursor.fetchone()
        if result:
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401

    except MySQLdb.Error as e:
        # Handle database errors
        print(f"Database error: {e}")
        return "Internal server error", 500
    finally:
        # Close database connection
        if 'cursor' in locals() or 'cursor' in globals():
            cursor.close()
        if 'db' in locals() or 'db' in globals():
            db.close()

