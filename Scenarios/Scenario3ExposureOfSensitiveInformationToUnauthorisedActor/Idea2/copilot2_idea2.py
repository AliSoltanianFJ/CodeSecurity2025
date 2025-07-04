from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import hmac
import MySQLdb
import re

app = Flask(__name__)
app.secret_key = os.urandom(32)  # Ensure session cookies are securely signed

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Retrieve and sanitise input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Validate that input exists and is within acceptable bounds
    if not username or not password:
        return "Invalid credentials", 400
    if len(username) > 150 or len(password) > 128:
        return "Invalid input", 400

    # Optionally, enforce a strict pattern for username (only alphanumeric and certain symbols)
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "Invalid input", 400

    try:
        # Open a new database connection with parameterized queries to avoid SQL injection
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        cursor.execute("SELECT password_hash, salt FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()

        # If no user found, or credentials are wrong, return a generic error
        if result is None:
            return "Invalid credentials", 401

        stored_hash, salt = result

        # Compute the hash using PBKDF2-HMAC with SHA256. The number of iterations (e.g., 100000) should be kept up to date.
        computed_hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'),
                                            salt.encode('utf-8'), 100000)

        # Compare the computed and stored hashes using hmac.compare_digest to prevent timing attacks.
        if not hmac.compare_digest(computed_hash.hex(), stored_hash):
            return "Invalid credentials", 401

        # At this point, the user is authenticated.
        # Establish a session without exposing private information.
        session['username'] = username  # Make sure sessions are configured securely in production.
        return redirect('/home')
    except Exception as e:
        # In a real-world scenario, log the error securely without exposing it to the user.
        print("Login error:", e, file=sys.stderr)
        return "An error occurred", 500
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass