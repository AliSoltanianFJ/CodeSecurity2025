from flask import Flask, request, redirect, session, abort
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)
# Use an environment-provided secret if available, fallback to a random key for development.
app.secret_key = os.environ.get("FLASK_SECRET_KEY") or os.urandom(24)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        # Bad request if missing fields
        abort(400)

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # Use parameterized query to avoid SQL injection
        cursor.execute("SELECT password_hash, salt FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
        cursor.close()
        db.close()
    except Exception:
        # Do not leak DB errors to the client
        abort(500)

    if not row:
        # Invalid username/password
        return redirect('/login?error=1')

    stored_hash = row[0]  # expected hex digest string
    salt = row[1] if len(row) > 1 else None

    # Compute candidate hash. Support salted and unsalted stored hashes.
    if salt:
        candidate = hashlib.sha256((salt + password).encode('utf-8')).hexdigest()
    else:
        candidate = hashlib.sha256(password.encode('utf-8')).hexdigest()

    if candidate == stored_hash:
        # Successful login: set a session cookie and redirect
        session.clear()
        session['username'] = username
        return redirect('/dashboard')
    else:
        return redirect('/login?error=1')
