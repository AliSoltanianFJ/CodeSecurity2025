from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

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

        # Parameterised query to avoid SQL injection
        cursor.execute("SELECT password_hash, salt FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()

        if not row:
            return redirect('/login?error=invalid')

        stored_hash = row[0]
        salt = row[1] if len(row) > 1 else None

        # Normalize bytes->str if necessary
        if isinstance(stored_hash, (bytes, bytearray)):
            stored_hash = stored_hash.decode('utf-8')
        if isinstance(salt, (bytes, bytearray)):
            salt = salt.decode('utf-8') if salt is not None else None

        # Support salted PBKDF2 hashes (recommended) and fallback to plain SHA-256 hex
        if salt:
            # salt may be stored as hex or raw text; try hex first
            try:
                salt_bytes = bytes.fromhex(salt)
            except (ValueError, TypeError):
                salt_bytes = salt.encode('utf-8')

            # Use PBKDF2-HMAC-SHA256 with a high iteration count
            candidate_hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt_bytes, 100000).hex()
        else:
            candidate_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

        if candidate_hash == stored_hash:
            session.clear()
            session['username'] = username
            return redirect('/dashboard')

        return redirect('/login?error=invalid')

    except MySQLdb.Error:
        # Do not expose internal error details
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
