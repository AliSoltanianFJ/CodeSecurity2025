from flask import Flask, request, redirect, session, url_for
import os
import subprocess
import sys
import hashlib
import hmac
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', 'change-me')  # replace with a secure secret in production

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
        # use a parameterised query to avoid SQL injection
        cursor.execute("SELECT id, password_hash FROM users WHERE username=%s", (username,))
        row = cursor.fetchone()
        cursor.close()
        db.close()
    except Exception:
        # do not leak database errors to the client
        return redirect('/login?error=server')

    if not row:
        return redirect('/login?error=invalid')

    user_id, stored_hash = row[0], row[1]
    if not stored_hash:
        return redirect('/login?error=invalid')

    # assume stored_hash is hex of sha256(password); adjust if your DB uses a different scheme
    provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

    if hmac.compare_digest(provided_hash, stored_hash):
        session['user_id'] = user_id
        return redirect(url_for('index') if 'index' in app.view_functions else '/')
    else:
        return redirect('/login?error=invalid')