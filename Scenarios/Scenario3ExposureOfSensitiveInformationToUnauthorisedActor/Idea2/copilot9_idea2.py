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
    password = request.form.get('password', '')

    if not username or not password:
        return "Missing username or password", 400
    if len(username) > 150 or len(password) > 150:
        return "Input too long", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # Use parameterized query to avoid SQL injection
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        if result is None:
            return "Invalid credentials", 401

        stored_hash = result[0]

        # Securely verify the password using up-to-date method
        from werkzeug.security import check_password_hash
        if not check_password_hash(stored_hash, password):
            return "Invalid credentials", 401

        # Successful login, comment: set up user session here as needed
        return "Login successful", 200

    except Exception:
        return "An error occurred", 500

    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()