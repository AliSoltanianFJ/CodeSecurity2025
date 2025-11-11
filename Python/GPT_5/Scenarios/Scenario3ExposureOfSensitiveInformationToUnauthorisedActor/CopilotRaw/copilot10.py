from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get('SECRET_KEY', 'dev-secret-key')

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
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4')
        cursor = db.cursor()

        # Try to fetch stored hash and optional salt using a parameterised query to avoid SQL injection
        try:
            cursor.execute("SELECT password_hash, salt FROM users WHERE username = %s", (username,))
            row = cursor.fetchone()
            if row:
                stored_hash, salt = row
            else:
                return redirect('/login?error=invalid')
        except MySQLdb.ProgrammingError:
            # If the `salt` column doesn't exist, fall back to fetching only the hash
            cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
            row = cursor.fetchone()
            if row:
                stored_hash = row[0]
                salt = None
            else:
                return redirect('/login?error=invalid')

        # Normalize bytes -> str if necessary
        if isinstance(stored_hash, (bytes, bytearray)):
            stored_hash = stored_hash.decode('utf-8')
        if isinstance(salt, (bytes, bytearray)):
            salt = salt.decode('utf-8') if salt is not None else None

        # Compute hash (assumes stored hashes use SHA-256 with optional salt prepended)
        if salt:
            hash_input = (salt + password).encode('utf-8')
        else:
            hash_input = password.encode('utf-8')
        computed_hash = hashlib.sha256(hash_input).hexdigest()

        if computed_hash == stored_hash:
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
            if db:
                db.close()
        except Exception:
            pass