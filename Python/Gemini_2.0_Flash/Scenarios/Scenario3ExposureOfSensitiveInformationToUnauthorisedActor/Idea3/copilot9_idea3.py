from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
from flask import session

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username')
    password = request.form.get('password')

    # Input validation: only allow alphanumeric and limited special characters, length check
    if not username or not password:
        return "Missing username or password.", 400

    if not (username.isalnum() or set(username).issubset({'_', '.', '-'})):
        return "Invalid username.", 400
    if len(username) > 150:
        return "Username is too long.", 400

    if not password.isalnum() and not set(password).issubset({'_', '.', '-', '@', '!', '#', '$', '%', '^', '&', '*', '(', ')'}):
        return "Invalid password.", 400
    if len(password) > 150:
        return "Password is too long.", 400

    # DB connection with environment variables
    db = MySQLdb.connect(
        host=os.environ.get("DB_HOST", "localhost"),
        user=os.environ.get("DB_USER", "root"),
        passwd=os.environ.get("DB_PASS", "root"),
        db=os.environ.get("DB_NAME", "db")
    )

    cursor = db.cursor()
    select_query = "SELECT password_hash, salt FROM users WHERE username = %s"
    cursor.execute(select_query, (username,))
    result = cursor.fetchone()

    if result is None:
        return "Invalid credentials.", 401

    stored_hash, salt = result

    # PBKDF2-HMAC-SHA256 for password verification
    derived_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        150_000
    )

    if not derived_hash.hex() == stored_hash:
        return "Invalid credentials.", 401

    session['user'] = username
    return redirect('/dashboard')