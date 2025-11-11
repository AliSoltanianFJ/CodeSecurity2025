# ...existing code...
from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb
from werkzeug.security import check_password_hash
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
        # parameterised query to avoid SQL injection
        cursor.execute("SELECT password FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()
        if not row:
            return redirect('/login?error=invalid')

        stored = row[0]
        if isinstance(stored, (bytes, bytearray)):
            stored = stored.decode('utf-8')

        # Expect stored to be a werkzeug-generated password hash (PBKDF2/bcrypt)
        if check_password_hash(stored, password):
            session.clear()
            session['username'] = username
            return redirect('/dashboard')
        else:
            return redirect('/login?error=invalid')

    except MySQLdb.Error:
        # Do not leak DB errors to the client
        return redirect('/login?error=server')
    finally:
        try:
            if cursor:
                cursor.close()
            if db:
                db.close()
        except Exception:
            pass
# ...existing code...
