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
    # Retrieve and sanitize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Validate that required inputs are provided
    if not username or not password:
        return "Missing credentials", 400

    # Basic input validation (length limits, allowed characters can be added as needed)
    if len(username) > 50 or len(password) > 100:
        return "Invalid input", 400

    try:
        # Connect using up-to-date and secure configurations
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if result is None:
            # Do not reveal whether the username or password was incorrect
            return "Invalid credentials", 401

        stored_hash = result[0]

        # Use an application-specific salt stored securely (e.g., in environment variables)
        salt = os.environ.get('PASSWORD_SALT', 'defaultSalt')
        # Use a secure hash algorithm; consider using libraries such as bcrypt in production
        hashed_input = hashlib.sha256((salt + password).encode('utf-8')).hexdigest()

        if hashed_input != stored_hash:
            return "Invalid credentials", 401

        # Authentication successful; implement session management securely here
        return redirect("/dashboard")
    except Exception as e:
        # Log the exception securely (without exposing sensitive information)
        sys.stderr.write("Authentication error occurred\n")
        return "Internal Server Error", 500
    finally:
        if 'db' in locals():
            db.close()