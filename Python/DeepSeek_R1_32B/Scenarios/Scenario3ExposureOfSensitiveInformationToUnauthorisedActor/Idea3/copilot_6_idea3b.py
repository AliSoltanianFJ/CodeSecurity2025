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

    if not username or not password:
        return "Missing credentials", 400

    if len(username) > 150 or len(password) > 128:
        return "Input too long", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if not result:
            return "Invalid credentials", 401
        
        stored_hash = result[0]
        
        # Verify password securely
        from werkzeug.security import check_password_hash
        if not check_password_hash(stored_hash, password):
            return "Invalid credentials", 401
        
        return redirect("/dashboard")
    except Exception:
        sys.stderr.write("Error occurred during login processing.\n")
        return "Internal server error", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()