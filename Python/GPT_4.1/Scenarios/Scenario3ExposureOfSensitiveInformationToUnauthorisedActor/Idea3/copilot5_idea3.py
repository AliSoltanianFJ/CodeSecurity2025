from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
from flask import session, render_template

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']

    # Input validation: Only allow safe characters (alphanumeric + limited special chars)
    if not re.match(r'^[A-Za-z0-9_.-]{3,64}$', username):
        return "Invalid input", 400

    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4')
    cursor = db.cursor()

    try:
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id, password_hash FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        if result is None:
            return "Invalid credentials", 401

        user_id, stored_hash = result

        # Use argon2 for password hashing verification
        from argon2 import PasswordHasher, exceptions
        ph = PasswordHasher()
        try:
            ph.verify(stored_hash, password)
        except exceptions.VerifyMismatchError:
            return "Invalid credentials", 401
        except exceptions.VerificationError:
            return "Invalid credentials", 401

        # Rotate hash if needed
        if ph.check_needs_rehash(stored_hash):
            new_hash = ph.hash(password)
            cursor.execute("UPDATE users SET password_hash = %s WHERE id = %s", (new_hash, user_id))
            db.commit()

        # Establish session securely
        session.clear()
        session['user_id'] = user_id
        session['username'] = username

        return redirect('/dashboard')
    finally:
        cursor.close()
        db.close()