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
    # Retrieve and validate input length and characters
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    
    if not username or not password:
        return "Missing credentials", 400

    if len(username) > 150 or len(password) > 128:
        return "Invalid input length", 400

    # Basic input sanitization (only allow alphanumeric and underscore)
    import re
    if not re.match(r'^[A-Za-z0-9_]+$', username):
        return "Invalid username", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT password_hash, salt FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()

        if result is None:
            return "Invalid credentials", 401

        stored_hash, salt = result
        
        # Verify password using PBKDF2
        import hashlib, hmac, binascii
        from werkzeug.security import check_password_hash

        if not check_password_hash(stored_hash, password + salt):
            return "Invalid credentials", 401

        # If authentication succeeds, proceed here...

    except Exception as e:
        print("Login error:", e, file=sys.stderr)
        return "An error occurred", 500
    finally:
        if 'db' in locals():
            db.close()

    return redirect('/dashboard')