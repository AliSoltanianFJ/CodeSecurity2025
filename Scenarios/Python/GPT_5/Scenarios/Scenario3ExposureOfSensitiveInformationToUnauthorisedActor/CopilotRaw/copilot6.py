from flask import Flask, request, redirect, session, abort
import os
import subprocess
import sys
import hashlib
import hmac
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get('SECRET_KEY', 'dev_secret')

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return "Missing username or password", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        # Use a parameterized query to avoid SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cursor.fetchone()
        if not row:
            # Don't reveal whether username or password was incorrect
            return "Invalid username or password", 401

        stored_hash = row[0]
        if isinstance(stored_hash, bytes):
            stored_hash = stored_hash.decode('utf-8')

        # Compute SHA-256 of supplied password (adjust if your DB uses a different scheme)
        supplied_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

        # Use constant-time comparison to avoid timing attacks
        if not hmac.compare_digest(stored_hash, supplied_hash):
            return "Invalid username or password", 401

        # Successful login: set session and redirect
        session['username'] = username
        return redirect('/dashboard')

    except Exception as e:
        # Avoid leaking internal errors to clients; log minimally to stderr
        print("Login error:", e, file=sys.stderr)
        return "Internal server error", 500

    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass
