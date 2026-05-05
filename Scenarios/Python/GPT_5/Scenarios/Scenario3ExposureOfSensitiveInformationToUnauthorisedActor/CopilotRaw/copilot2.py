# ...existing code...
from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)
app.secret_key = os.urandom(24)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    if not username or not password:
        return redirect('/login?error=missing')

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # use parameterized query to avoid SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
    except Exception:
        # do not expose internal error details to the user
        return redirect('/login?error=server')
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
        return redirect('/login?error=invalid')

    stored_hash = row[0]  # assume stored as hex SHA-256
    supplied_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

    if supplied_hash == stored_hash:
        session['username'] = username
        return redirect('/dashboard')

    return redirect('/login?error=invalid')
# ...existing code...
