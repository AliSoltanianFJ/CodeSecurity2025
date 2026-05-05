# ...existing code...
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
    password = request.form.get('password', '')
    if not username or not password:
        return redirect('/login?error=missing')

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # Use a parameterised query to avoid SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
        cursor.close()
        db.close()
    except Exception:
        # Avoid leaking internal error details to the client
        return redirect('/login?error=server')

    if not row:
        return redirect('/login?error=invalid')

    stored_hash = row[0]
    # Hash the provided password using SHA-256 and compare
    provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
    if provided_hash == stored_hash:
        # Successful login — set a minimal, HttpOnly cookie (secure=False for local dev)
        resp = redirect('/welcome')
        resp.set_cookie('username', username, httponly=True, secure=False)
        return resp

    return redirect('/login?error=invalid')
# ...existing code...
