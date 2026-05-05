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
    # Retrieve and validate input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        return "Missing credentials", 400

    if len(username) > 150 or len(password) > 128:
        return "Input too long", 400

    # Basic input sanitization (only allow alphanumeric and common safe characters)
    import re
    safe_username_pattern = re.compile(r"^[a-zA-Z0-9_.-]+$")
    if not safe_username_pattern.match(username):
        return "Invalid username", 400

    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    try:
        # Use parameterized query to prevent SQL injection
        select_query = "SELECT password_hash, salt FROM users WHERE username = %s"
        cursor.execute(select_query, (username,))
        result = cursor.fetchone()

        if result is None:
            return "Invalid credentials", 401

        stored_hash, salt = result
        
        # Verify password securely using PBKDF2
        import hashlib
        from hmac import compare_digest
        computed_hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt.encode('utf-8'), 100000)
        
        if not compare_digest(computed_hash, stored_hash):
            return "Invalid credentials", 401

        # Successful login: redirect to dashboard or home
        return redirect('/dashboard')

    except Exception as e:
        print("Login error:", e, file=sys.stderr)
        return "An error occurred", 500

    finally:
        cursor.close()
        db.close()