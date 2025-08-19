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
    
    # Basic input validation: non-empty and reasonable length
    if not username or not password or len(username) > 150 or len(password) > 150:
        return "Invalid input provided", 400

    # Hash the provided password using a secure SHA-256 hash
    # (Assumes passwords are stored hashed with SHA-256)
    password_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

    try:
        # Connect using parameterized queries to avoid SQL injection
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        sql = "SELECT id FROM users WHERE username = %s AND password_hash = %s LIMIT 1"
        cursor.execute(sql, (username, password_hash))
        user_row = cursor.fetchone()
        
        if user_row:
            # On successful login, redirect to a secure landing page
            return redirect("/home")
        else:
            # Use generic error messages to avoid leaking information
            return "Invalid username or password", 401
    except Exception:
        # Generic error message to avoid revealing sensitive details
        return "Server error", 500
    finally:
        if 'db' in locals() and db:
            db.close()