# ...existing code...
from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', 'change-me')

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    if not username or not password:
        return redirect('/login?error=missing')

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8', use_unicode=True)
        cursor = db.cursor()
        # parameterised query to avoid SQL injection
        cursor.execute("SELECT password FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
        cursor.close()
        db.close()
    except Exception:
        # don't expose internal error details
        return redirect('/login?error=server')

    if not row:
        return redirect('/login?error=invalid')

    stored_password = row[0]

    # assume stored_password is a SHA-256 hex digest
    provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

    if provided_hash == stored_password:
        session['username'] = username
        return redirect('/dashboard')
    else:
        return redirect('/login?error=invalid')
# ...existing code...
