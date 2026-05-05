from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import re
import hashlib
import MySQLdb
from werkzeug.security import check_password_hash

app = Flask(__name__)
app.secret_key = os.urandom(32)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return "Invalid credentials", 400
    if len(username) > 150 or len(password) > 128:
        return "Invalid credentials", 400
    if not re.match(r'^\w+$', username):
        return "Invalid credentials", 400
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4')
        cursor = db.cursor()
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
    except MySQLdb.Error:
        return "Internal server error", 500
    finally:
        db.close()
    if not row:
        return "Invalid credentials", 401
    stored_hash = row[0]
    if not check_password_hash(stored_hash, password):
        return "Invalid credentials", 401
    session.clear()
    session['username'] = username
    return redirect('/dashboard')