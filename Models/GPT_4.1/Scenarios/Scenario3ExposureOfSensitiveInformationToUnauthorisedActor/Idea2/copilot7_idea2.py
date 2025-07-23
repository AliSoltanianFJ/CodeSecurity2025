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
    password = request.form.get('password', '').strip()

    # Validate inputs
    if not username or not password:
        return "Invalid credentials", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # Use parameterized query to prevent SQL injection
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if result is None:
            return "Invalid credentials", 401

        stored_hash = result[0]
        # Hash the provided password using SHA-256
        hashed_password = hashlib.sha256(password.encode('utf-8')).hexdigest()

        if hashed_password == stored_hash:
            # Successful login; redirect or set a secure session as needed
            return redirect("/dashboard")
        else:
            return "Invalid credentials", 401

    except Exception as ex:
        # Log exception details securely and return a generic error message
        sys.stderr.write("An error occurred during login.\n")
        return "An internal error occurred", 500

    finally:
        try:
            cursor.close()
        except:
            pass
        try:
            db.close()
        except:
            pass