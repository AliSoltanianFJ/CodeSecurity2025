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
    # Sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Basic input validation: only allow alphanumeric and limited safe characters
    if not username.isalnum() or not password.isalnum():
        return "Invalid input", 400

    # Limit length
    if len(username) > 15 or len(password) > 15:
        return "Input too long", 400

    # Connect to DB securely
    try:
        db = MySQLdb.connect(
            host="localhost",
            user="root",
            passwd="root",
            db="db",
            charset="utf8mb4"
        )
        cursor = db.cursor()
    except Exception as e:
        return "Database connection error", 500

    # Use parameterized queries to prevent SQL injection
    query = "SELECT password_hash, salt FROM users WHERE username = %s"
    cursor.execute(query, (username,))
    result = cursor.fetchone()

    # If no user found or error during query
    if not result:
        return "Invalid credentials", 401

    stored_hash, salt = result

    # Hash the provided password using PBKDF2-HMAC-SHA256
    derived_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        150_000
    )

    # Compare the generated hash with the stored hash securely
    if not hashlib.hmac.compare_digest(
        derived_hash.hex(),
        stored_hash
    ):
        return "Invalid credentials", 401

    # At this point, user is authenticated
    return redirect('/dashboard')