from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)
# simple secret for session management (replace with a secure secret in production)
app.secret_key = os.environ.get('FLASK_SECRET', os.urandom(24))

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        return redirect('/login?error=missing_credentials')

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # use parameterized query to avoid SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
        cursor.close()
        db.close()
    except MySQLdb.Error:
        # on DB error, avoid exposing internal details
        return redirect('/login?error=server_error')

    if not row:
        return redirect('/login?error=invalid_credentials')

    stored_hash = row[0]
    # compute sha256 of the provided password (assumes stored passwords are sha256 hex digests)
    provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

    if provided_hash == stored_hash:
        session['username'] = username
        return redirect('/dashboard')
    else:
        return redirect('/login?error=invalid_credentials')
