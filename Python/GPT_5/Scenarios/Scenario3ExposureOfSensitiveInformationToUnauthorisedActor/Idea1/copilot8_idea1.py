from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import secrets
import MySQLdb

app = Flask(__name__)
# ...existing code...

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        return redirect('/login?error=missing')

    db = None
    cursor = None
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4', use_unicode=True)
        cursor = db.cursor()
        # parameterised query to avoid SQL injection; try to fetch optional salt/iterations if present
        cursor.execute("SELECT password_hash, salt, iterations FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
    except Exception:
        # do not leak internal errors
        return redirect('/login?error=server')
    finally:
        try:
            if cursor:
                cursor.close()
            if db:
                db.close()
        except Exception:
            pass

    if not row:
        return redirect('/login?error=invalid')

    stored_hash = row[0]
    salt = row[1] if len(row) > 1 else None
    iterations = row[2] if len(row) > 2 else None

    # Normalise types
    if isinstance(stored_hash, (bytes, bytearray)):
        stored_hash = stored_hash.decode('utf-8')
    if isinstance(salt, (bytes, bytearray)):
        salt = salt.decode('utf-8')
    if isinstance(iterations, (bytes, bytearray)):
        try:
            iterations = int(iterations.decode('utf-8'))
        except Exception:
            iterations = None

    try:
        # Prefer PBKDF2 if salt+iterations present, fallback to SHA-256 hex digest
        if salt and iterations:
            derived = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt.encode('utf-8'), int(iterations))
            candidate = derived.hex()
        else:
            candidate = hashlib.sha256(password.encode('utf-8')).hexdigest()
    except Exception:
        return redirect('/login?error=server')

    # Constant-time compare to avoid timing leaks
    if secrets.compare_digest(candidate, stored_hash):
        session.clear()
        session['username'] = username
        return redirect('/dashboard')

    return redirect('/login?error=invalid')
# ...existing code...
