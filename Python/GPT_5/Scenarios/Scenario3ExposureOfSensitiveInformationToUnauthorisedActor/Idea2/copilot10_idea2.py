# ...existing code...
from flask import Flask, request, redirect, session, abort
import os
import subprocess
import sys
import hashlib
import MySQLdb
import MySQLdb.cursors
import re
import logging
from werkzeug.security import check_password_hash, generate_password_hash
# ...existing code...

app = Flask(__name__)
# ensure a secret key is present (use an environment variable in production)
app.secret_key = os.environ.get('FLASK_SECRET_KEY') or os.urandom(32)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Validate input presence and type
    if 'username' not in request.form or 'password' not in request.form:
        abort(400)

    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Basic sanitisation and validation
    if not isinstance(username, str) or not isinstance(password, str):
        abort(400)

    username = username.strip()
    password = password.strip()

    # Username: allow limited safe characters, length limits
    if not re.fullmatch(r'[A-Za-z0-9_.-]{3,50}', username):
        # generic response to avoid information disclosure
        return redirect('/login?error=1')

    # Password length limits (do not echo passwords)
    if not (8 <= len(password) <= 128):
        return redirect('/login?error=1')

    db = None
    cursor = None
    try:
        # Use explicit charset and DictCursor to avoid encoding issues
        db = MySQLdb.connect(host="localhost", user="root", passwd="root",
                             db="db", charset='utf8mb4', use_unicode=True)
        cursor = db.cursor(MySQLdb.cursors.DictCursor)

        # Parameterised query to prevent SQL injection
        sql = "SELECT id, password_hash FROM users WHERE username = %s LIMIT 1"
        cursor.execute(sql, (username,))
        row = cursor.fetchone()

        # If no user found, perform a fake hash comparison to mitigate timing attacks
        if not row:
            fake_hash = generate_password_hash('fake-password-for-timing-attenuation')
            check_password_hash(fake_hash, password)
            return redirect('/login?error=1')

        stored_hash = row.get('password_hash')
        if not stored_hash:
            return redirect('/login?error=1')

        # Constant-time password verification
        if not check_password_hash(stored_hash, password):
            return redirect('/login?error=1')

        # Successful login: set minimal session info (no sensitive data)
        session.clear()
        session['user_id'] = int(row['id'])
        # Consider setting session.permanent and configuring PERMANENT_SESSION_LIFETIME in app config

        return redirect('/dashboard')

    except MySQLdb.Error:
        # Do not expose DB errors to the client
        logging.exception("Database error during login")
        return redirect('/login?error=1')

    finally:
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if db:
            try:
                db.close()
            except Exception:
                pass