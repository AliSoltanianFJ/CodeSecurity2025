# ...existing code...
from flask import Flask, request, redirect, session, abort
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get('SECRET_KEY', 'change-me-in-production')

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return "Missing username or password", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # use parameterized query to avoid SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
    except MySQLdb.Error:
        return "Database error", 500
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass

    if not row:
        return "Invalid username or password", 401

    stored_hash = row[0]
    # compare SHA-256 hashes (assumes the DB stores hex-encoded sha256(password))
    provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
    if provided_hash != stored_hash:
        return "Invalid username or password", 401

    # successful login: set session and redirect
    session['username'] = username
    return redirect('/dashboard')
# ...existing code...
