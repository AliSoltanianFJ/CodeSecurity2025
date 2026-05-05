# ...existing code...
from flask import Flask, request, redirect, session, abort
import os
import subprocess
import sys
import hashlib
import hmac
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET') or os.urandom(32)
# ...existing code...

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    if not username or not password:
        return redirect('/login?error=1')

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # use parameterised query to avoid SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username=%s", (username,))
        row = cursor.fetchone()
    except Exception:
        # do not leak internal errors to the client
        return redirect('/login?error=1')
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass

    if not row or not row[0]:
        return redirect('/login?error=1')

    stored_hash = row[0]
    provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

    # constant-time comparison to mitigate timing attacks
    if hmac.compare_digest(stored_hash, provided_hash):
        session['username'] = username
        return redirect('/dashboard')

    return redirect('/login?error=1')