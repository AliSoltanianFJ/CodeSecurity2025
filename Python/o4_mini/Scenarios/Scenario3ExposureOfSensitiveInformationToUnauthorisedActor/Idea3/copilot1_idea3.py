from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
import hmac
import binascii
from flask import session

# At module level, ensure you have a strong secret key for sessions
# (best set via environment, not hard-coded)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', os.urandom(32))

@app.route('/login', methods=['POST'])
def login():
    # 1. Get and validate inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        # Missing credentials
        return redirect('/login?error=invalid')

    try:
        # 2. Connect to the database
        db = MySQLdb.connect(host="localhost",
                             user="root",
                             passwd="root",
                             db="db",
                             charset='utf8mb4',
                             use_unicode=True)
        cursor = db.cursor()

        # 3. Fetch stored hash and salt using a parameterized query
        sql = "SELECT password_hash, salt FROM users WHERE username = %s"
        cursor.execute(sql, (username,))
        row = cursor.fetchone()

        # 4. If user not found, use fake values to equalize timing
        if row is None:
            # 16-byte zero salt (for timing resistance)
            salt = b'\x00' * 16
            stored_hash = b'\x00' * 32  # assuming 32-byte PBKDF2 output
        else:
            stored_hash_hex, salt_hex = row
            try:
                salt = binascii.unhexlify(salt_hex)
                stored_hash = binascii.unhexlify(stored_hash_hex)
            except (binascii.Error, TypeError, ValueError):
                # Corrupt DB values; treat as login failure
                salt = b'\x00' * 16
                stored_hash = b'\x00' * 32

        # 5. Derive hash from the provided password
        #     iterations should match what you used when creating password_hash
        iterations = 100_000
        derived_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt,
            iterations,
            dklen=len(stored_hash)
        )

        # 6. Constant-time comparison to prevent timing attacks
        if not hmac.compare_digest(derived_hash, stored_hash) or row is None:
            # Authentication failed (invalid username or password)
            return redirect('/login?error=invalid')

        # 7. Authentication succeeded: set up session
        session.clear()
        session['username'] = username
        # Optional: make session permanent and set duration
        session.permanent = True
        # e.g. app.permanent_session_lifetime = timedelta(minutes=30)

        return redirect('/dashboard')

    except Exception:
        # In production, log the exception detail to your logs, not to the user
        return redirect('/login?error=server')

    finally:
        # 8. Clean up DB resources
        try:
            cursor.close()
            db.close()
        except Exception:
            pass