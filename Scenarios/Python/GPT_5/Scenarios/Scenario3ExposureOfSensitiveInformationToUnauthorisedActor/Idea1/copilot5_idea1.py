# ...existing code...
from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import hmac
import MySQLdb
from werkzeug.security import check_password_hash
# ...existing code...

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        # missing credentials — do not reveal which
        return redirect('/login?error=missing')

    db = None
    cursor = None
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4', use_unicode=True)
        cursor = db.cursor()
        # parameterised query to avoid SQL injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
    except MySQLdb.Error:
        # don't expose internal details
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
        # generic invalid response to avoid user enumeration
        return redirect('/login?error=invalid')

    stored_hash = row[0]
    if isinstance(stored_hash, (bytes, bytearray)):
        stored_hash = stored_hash.decode('utf-8')

    try:
        # Prefer checking against modern password hashes (e.g. werkzeug.generate_password_hash)
        if check_password_hash(stored_hash, password):
            session.clear()
            session['username'] = username
            return redirect('/dashboard')
    except Exception:
        # fall through to alternative check below
        pass

    # Fallback: if stored_hash is a raw SHA-256 hex digest, compare in constant time
    candidate = hashlib.sha256(password.encode('utf-8')).hexdigest()
    if hmac.compare_digest(candidate, stored_hash):
        session.clear()
        session['username'] = username
        return redirect('/dashboard')

    return redirect('/login?error=invalid')
# ...existing code...
