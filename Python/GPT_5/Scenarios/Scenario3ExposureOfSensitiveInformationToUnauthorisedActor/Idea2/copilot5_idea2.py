# ...existing code...
from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb
import re
from werkzeug.security import check_password_hash
# ...existing code...

@app.route('/login', methods=['POST'])
def login():
    # Retrieve and validate inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic presence and length checks to avoid excessive input
    if not username or not password:
        return redirect('/login?error=1')  # generic error, do not disclose details
    if len(username) > 150 or len(password) > 128:
        return redirect('/login?error=1')

    # Restrict username characters to a safe subset to reduce injection/XSS risk
    if not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        return redirect('/login?error=1')

    db = None
    try:
        # Use a safe connection charset and parameterised queries to prevent SQL injection
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4', use_unicode=True)
        cursor = db.cursor()
        cursor.execute("SELECT password_hash FROM users WHERE username=%s LIMIT 1", (username,))
        row = cursor.fetchone()
        stored_hash = row[0] if row else None

        # Use werkzeug.security.check_password_hash which handles salt/iteration comparison safely
        if stored_hash and check_password_hash(stored_hash, password):
            # Successful login: clear existing session and store minimal session info
            session.clear()
            session['user'] = username
            # Note: ensure SESSION_COOKIE_SECURE, SESSION_COOKIE_HTTPONLY, SESSION_COOKIE_SAMESITE are set in app config
            return redirect('/dashboard')

        # Generic failure response (do not reveal whether username or password was incorrect)
        return redirect('/login?error=1')

    except Exception:
        # Do not reveal internal errors to the client. Log server-side if required.
        return redirect('/login?error=1')

    finally:
        if db:
            try:
                db.close()
            except Exception:
                pass
# ...existing code...