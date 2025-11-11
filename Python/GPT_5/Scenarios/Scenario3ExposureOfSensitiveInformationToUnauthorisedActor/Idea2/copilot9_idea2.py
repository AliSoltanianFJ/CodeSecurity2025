# ...existing code...
from flask import Flask, request, redirect, session, abort
import os
import subprocess
import sys
import hashlib
import MySQLdb
from werkzeug.security import check_password_hash
import re
# ...existing code...
app = Flask(__name__)
app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_SAMESITE='Lax'
)
# ...existing code...
"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Basic presence and size validation to prevent abuse / buffer issues
    if not username or not password:
        return "Invalid credentials", 400
    username = username.strip()
    if len(username) > 150 or len(password) > 128:
        return "Invalid credentials", 400

    # Restrict allowed username characters to reduce injection/XSS surface
    if not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        return "Invalid credentials", 400

    # Require a configured secret key to avoid insecure session handling
    if not app.secret_key:
        app.logger.error("Missing FLASK_SECRET_KEY; refusing authentication to avoid insecure sessions")
        return "Server configuration error", 500

    db = None
    cursor = None
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4', use_unicode=True)
        cursor = db.cursor()
        # Parameterized query prevents SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username=%s LIMIT 1", (username,))
        row = cursor.fetchone()
        if not row or not row[0]:
            # Do not reveal whether username or password was wrong
            return "Invalid credentials", 401

        stored_hash = row[0]
        if isinstance(stored_hash, (bytes, bytearray)):
            stored_hash = stored_hash.decode('utf-8', errors='ignore')

        # Use a modern, safe password hash verifier (werkzeug). This avoids timing attacks.
        if check_password_hash(stored_hash, password):
            # Successful login: set a minimal session marker (do not store sensitive info)
            session.clear()
            session['username'] = username
            # Redirect only to an internal endpoint to prevent open redirect
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401

    except MySQLdb.Error:
        app.logger.exception("Database error during login")
        return "Server error", 500
    finally:
        try:
            if cursor:
                cursor.close()
        except Exception:
            pass
        try:
            if db:
                db.close()
        except Exception:
            pass
# ...existing code...
