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
    username = request.form['username']
    password = request.form['password']

    # Validate input length (simple check)
    if not username or not password or len(username) > 150 or len(password) > 150:
        return "Invalid input", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT password FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
    except Exception:
        return "Internal server error", 500
    finally:
        cursor.close()
        db.close()

    if result is None:
        return "Invalid credentials", 401

    stored_hash = result[0]

    # Use werkzeug.security to check password hash (assuming password is hashed)
    from werkzeug.security import check_password_hash
    if check_password_hash(stored_hash, password):
        from flask import session
        session['user'] = username
        return redirect('/dashboard')
    else:
        return "Invalid credentials", 401