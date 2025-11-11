from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import hmac
import MySQLdb

app = Flask(__name__)


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
        # parameterised query to avoid SQL injection
        cursor.execute("SELECT password_hash, salt, iterations, algo FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
    except MySQLdb.Error:
        # do not leak DB errors to the client
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

    if not row:
        return redirect('/login?error=invalid')

    stored_hash = row[0]
    salt = row[1] if len(row) > 1 else None
    iterations = row[2] if len(row) > 2 else None
    algo = row[3] if len(row) > 3 else None

    # Normalize types
    if isinstance(stored_hash, (bytes, bytearray)):
        stored_hash = stored_hash.decode('utf-8')
    if isinstance(salt, (bytes, bytearray)):
        salt_bytes = salt
    else:
        salt_bytes = salt.encode('utf-8') if salt is not None else None

    password_bytes = password.encode('utf-8')

    try:
        # Support PBKDF2 (preferred) if stored as such, else fallback to salted/unsalted SHA-256
        if algo == 'pbkdf2_sha256' or iterations:
            iter_count = int(iterations) if iterations else 100000
            candidate = hashlib.pbkdf2_hmac('sha256', password_bytes, salt_bytes or b'', iter_count).hex()
        else:
            if salt_bytes:
                candidate = hashlib.sha256(salt_bytes + password_bytes).hexdigest()
            else:
                candidate = hashlib.sha256(password_bytes).hexdigest()
    except Exception:
        return redirect('/login?error=server')

    # Constant-time comparison
    if hmac.compare_digest(candidate, stored_hash):
        session.clear()
        session['username'] = username
        return redirect('/dashboard')

    return redirect('/login?error=invalid')

