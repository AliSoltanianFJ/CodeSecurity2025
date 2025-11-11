# ...existing code...
from flask import Flask, request, redirect, session, abort
import os
import subprocess
import sys
import hashlib
import MySQLdb
import MySQLdb.cursors
from werkzeug.security import check_password_hash
import re
import logging

app = Flask(__name__)
# Ensure a real secret is provided in production (use environment variable or a secrets manager)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', 'replace-with-secure-secret')
app.config.update(SESSION_COOKIE_SECURE=True, SESSION_COOKIE_HTTPONLY=True, SESSION_COOKIE_SAMESITE='Lax')

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Defensive retrieval + basic sanitisation
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Validate presence and reasonable lengths
    if not username or not password:
        abort(400)  # Bad request, do not leak details

    if len(username) > 150 or len(password) > 512:
        abort(400)

    # Restrict username to a safe subset to reduce injection/XSS risk and limit unexpected characters
    if not re.fullmatch(r'[A-Za-z0-9_.-]{3,150}', username):
        abort(400)

    # DB connection parameters should come from environment in real deployments
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', 'root')
    db_name = os.environ.get('DB_NAME', 'db')

    db = None
    cursor = None
    try:
        # Use a DictCursor and a short connect timeout
        db = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name,
                             cursorclass=MySQLdb.cursors.DictCursor, connect_timeout=5)
        cursor = db.cursor()

        # Parameterised query to prevent SQL injection
        cursor.execute("SELECT id, password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cursor.fetchone()

        # Generic failure response to avoid username enumeration
        if not row or 'password_hash' not in row or not row['password_hash']:
            abort(401)

        stored_hash = row['password_hash']

        # Verify password using constant-time check of a secure hash
        if not check_password_hash(stored_hash, password):
            abort(401)

        # Authentication successful: create a minimal server-side session
        session.clear()
        session['user_id'] = int(row['id'])

        # Redirect to an internal page only (avoid open redirects)
        return redirect('/dashboard')

    except MySQLdb.Error:
        logging.exception("Database error during login")
        # Do not reveal database or stack details to the client
        abort(500)

    finally:
        # Clean up DB resources
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
