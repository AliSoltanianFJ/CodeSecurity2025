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
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Input validation: Check length and allowed characters
    if not (1 <= len(username) <= 32 and 1 <= len(password) <= 64):
        return "Invalid input", 400

    # Only allow alphanumeric usernames for safety
    import re
    if not re.match(r'^[A-Za-z0-9_]+$', username):
        return "Invalid username", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if result is None:
            return "Invalid credentials", 401

        stored_hash = result[0]

        # Use a secure hash function for password verification
        import bcrypt
        if not bcrypt.checkpw(password.encode('utf-8'), stored_hash):
            return "Invalid credentials", 401

        # If authentication is successful, you can proceed to create a session or return a token here.

    except Exception as e:
        print("Login error:", e, file=sys.stderr)
        return "Internal server error", 500
    finally:
        if 'db' in locals():
            db.close()

    return redirect('/dashboard')