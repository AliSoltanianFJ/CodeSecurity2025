from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']

    # Connect to the database
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()

    try:
        # Query the database for the user's password hash
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()

        if not result:
            # No such user exists
            return "Invalid username or password", 401

        stored_hash = result[0]

        # Compute the hash of the provided password
        input_hash = hashlib.sha256(password.encode()).hexdigest()

        if input_hash == stored_hash:
            # Set session variable and redirect to dashboard
            session['username'] = username
            return redirect('/dashboard')
        else:
            return "Invalid username or password", 401

    except MySQLdb.Error as e:
        # Handle database errors
        return f"Database error: {e}", 500
    finally:
        cursor.close()
        db.close()

# Ensure the session is configured in your Flask app
app.secret_key = os.urandom(24)

