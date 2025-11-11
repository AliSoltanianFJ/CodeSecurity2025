# ...existing code...
from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import hmac
import binascii
import re
import MySQLdb

app = Flask(__name__)
# ensure secret is provided via environment in production
app.secret_key = os.environ.get('FLASK_SECRET') or os.urandom(32)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Validate presence and basic shape of inputs
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    # Basic validation: allowed username chars and reasonable lengths
    if not username or not password:
        # Generic response to avoid user enumeration
        return "Invalid credentials", 400

    if len(username) > 150 or not re.fullmatch(r"[A-Za-z0-9_.-]{1,150}", username):
        return "Invalid credentials", 400

    # Prevent very large passwords (DoS / excessive memory use)
    if len(password) > 4096:
        return "Invalid credentials", 400

    try:
        # Use parameterized queries to prevent SQL injection
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cur = db.cursor()
        cur.execute(
            "SELECT id, password_hash, salt, iterations FROM users WHERE username=%s LIMIT 1",
            (username,)
        )
        row = cur.fetchone()
        # Prepare default/fake values to avoid timing differences on missing user
        auth_ok = False
        user_id = None

        if row:
            user_id, stored_hash, stored_salt, stored_iters = row
            # Handle stored formats robustly
            # If salt and iterations are present, assume PBKDF2-HMAC-SHA256
            if stored_salt and stored_iters:
                # stored_salt may be bytes or hex string
                if isinstance(stored_salt, str):
                    try:
                        salt_bytes = binascii.unhexlify(stored_salt)
                    except (binascii.Error, TypeError):
                        salt_bytes = stored_salt.encode('utf-8')
                else:
                    # already bytes-like
                    salt_bytes = stored_salt

                try:
                    iterations = int(stored_iters)
                except Exception:
                    iterations = 100_000

                derived = hashlib.pbkdf2_hmac(
                    'sha256',
                    password.encode('utf-8'),
                    salt_bytes,
                    iterations
                )

                # stored_hash may be hex string or bytes
                if isinstance(stored_hash, str):
                    try:
                        stored_hash_bytes = binascii.unhexlify(stored_hash)
                    except (binascii.Error, TypeError):
                        stored_hash_bytes = stored_hash.encode('utf-8')
                else:
                    stored_hash_bytes = stored_hash

                auth_ok = hmac.compare_digest(derived, stored_hash_bytes)
            else:
                # Fallback: compare SHA-256 hex digests (legacy)
                digest = hashlib.sha256(password.encode('utf-8')).hexdigest()
                if isinstance(stored_hash, bytes):
                    try:
                        stored_hash = stored_hash.decode('utf-8')
                    except Exception:
                        stored_hash = binascii.hexlify(stored_hash).decode('utf-8')
                auth_ok = hmac.compare_digest(digest, stored_hash)

        else:
            # Fake work to mitigate timing/user enumeration
            hashlib.pbkdf2_hmac('sha256', b'fakepassword', b'0'*16, 100_000)

        cur.close()
        db.close()
    except Exception:
        # Never leak internal errors or sensitive details
        return "An error occurred", 500

    if not auth_ok:
        # Generic failure response
        return "Invalid credentials", 401

    # Successful login: set a server-side session identifier
    session.clear()
    session['user_id'] = int(user_id)
    # Do not include any private info in the redirect / response
    return redirect('/dashboard')
# ...existing code...
