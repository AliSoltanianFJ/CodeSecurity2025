# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
import re
from flask import session
from werkzeug.security import check_password_hash

app = Flask(__name__)

# secure session configuration (use environment-provided secret in production)
app.secret_key = os.environ.get('FLASK_SECRET_KEY') or os.urandom(32)
app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_SAMESITE='Lax',
)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # retrieve and minimally sanitize inputs
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    # Basic validation to prevent abuse and injection attempts
    if not username or not password:
        return redirect('/login?error=invalid')

    # enforce reasonable length limits
    if len(username) > 150 or len(password) > 1024:
        return redirect('/login?error=invalid')

    # allow a conservative set of characters for usernames
    if not re.fullmatch(r'[A-Za-z0-9_.@\-]+', username):
        return redirect('/login?error=invalid')

    # encourage strong passwords (server-side minimal check)
    if len(password) < 8:
        return redirect('/login?error=invalid')

    db = None
    cursor = None
    try:
        # use parameterized queries to prevent SQL injection
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4')
        cursor = db.cursor()
        cursor.execute("SELECT id, password_hash FROM users WHERE username=%s LIMIT 1", (username,))
        row = cursor.fetchone()
        if not row:
            # do not reveal whether the username exists
            return redirect('/login?error=invalid')

        user_id, password_hash = row

        # verify password using safe, time-constant checker
        if not password_hash or not check_password_hash(password_hash, password):
            return redirect('/login?error=invalid')

        # authentication successful: establish session without exposing private data
        session.clear()
        session['user_id'] = int(user_id)

        return redirect('/dashboard')

    except Exception:
        # avoid leaking internal errors or stack traces to the client
        return redirect('/login?error=server')

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
