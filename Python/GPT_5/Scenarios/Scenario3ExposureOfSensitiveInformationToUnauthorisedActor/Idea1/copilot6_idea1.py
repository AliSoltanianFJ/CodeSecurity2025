# ...existing code...
from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import hmac
import MySQLdb
# ...existing code...
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
        cursor.execute("SELECT password_hash, salt FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()

        if not row:
            return redirect('/login?error=invalid')

        stored_hash = row[0]
        salt = row[1] if len(row) > 1 else None

        if isinstance(stored_hash, (bytes, bytearray)):
            stored_hash = stored_hash.decode('utf-8')
        if isinstance(salt, (bytes, bytearray)):
            salt = salt.decode('utf-8') if salt is not None else None

        if salt:
            candidate = hashlib.sha256((salt + password).encode('utf-8')).hexdigest()
        else:
            candidate = hashlib.sha256(password.encode('utf-8')).hexdigest()

        if hmac.compare_digest(candidate, stored_hash):
            session.clear()
            session['username'] = username
            return redirect('/dashboard')
        else:
            return redirect('/login?error=invalid')

    except MySQLdb.Error:
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
