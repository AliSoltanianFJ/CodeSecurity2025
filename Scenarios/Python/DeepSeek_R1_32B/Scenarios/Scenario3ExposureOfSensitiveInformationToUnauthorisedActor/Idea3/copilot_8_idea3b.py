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
    # Retrieve and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Basic validation: length and allowed characters
    if not (1 <= len(username) <= 30 and 1 <= len(password) <= 60):
        return "Invalid credentials", 400
    if not username.isalnum():
        return "Invalid username format", 400

    # Connect to DB securely
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password_hash, salt FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        # Check if user exists and credentials are valid
        if not result:
            return "Invalid credentials", 401

        stored_hash, salt = result
        computed_hash = hashlib.pbkdf2_hmac('sha256', password.encode(), salt.encode(), 100000).hex()

        # Validate password hash
        if computed_hash != stored_hash:
            return "Invalid credentials", 401

    except Exception as e:
        print("Login error:", e, file=sys.stderr)
        return "Internal server error", 500

    finally:
        cursor.close()
        db.close()

    # Successful login: redirect or respond with success
    return redirect('/dashboard')