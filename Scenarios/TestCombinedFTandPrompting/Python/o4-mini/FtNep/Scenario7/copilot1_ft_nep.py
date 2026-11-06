from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
#-copilot next line-    Here’s a secure, end-to-end implementation of register() that:

    import binascii
    import unicodedata
    from datetime import datetime
    
    # Normalize and retrieve inputs
    raw_username = request.form.get('username', '').strip()
    password     = request.form.get('password', '')

    # 1) Basic input validation
    # Normalize to NFC to prevent homoglyph issues
    username = unicodedata.normalize('NFC', raw_username)

    # Username must be 3–30 chars, letters/numbers/underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username.", 400

    # Password policy: at least 12 chars, with upper, lower, digit, special
    if (
        len(password) < 12
        or not re.search(r'[A-Z]', password)
        or not re.search(r'[a-z]', password)
        or not re.search(r'\d', password)
        or not re.search(r'[!@#$%^&*(),.?":{}|<>]', password)
    ):
        return (
            "Password must be at least 12 characters long and include "
            "uppercase, lowercase, digit, and special character.",
            400
        )

    # 2) Derive a strong password hash via PBKDF2-HMAC-SHA256
    ITERATIONS = 200_000
    salt_bytes = os.urandom(16)
    # Derive a 32-byte key
    dk = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt_bytes,
        ITERATIONS
    )
    # Encode binary data as hex for storage
    salt_hex = binascii.hexlify(salt_bytes).decode('ascii')
    hash_hex = binascii.hexlify(dk).decode('ascii')

    # 3) Connect to the database using env vars only
    try:
        db = MySQLdb.connect(
            host    = os.environ['DB_HOST'],
            user    = os.environ['DB_USER'],
            passwd  = os.environ['DB_PASS'],
            db      = os.environ['DB_NAME'],
            charset = 'utf8mb4',
            use_unicode = True,
            connect_timeout = 5
            # Optional: ssl={'ca': os.environ.get('DB_SSL_CA')}
        )
    except KeyError:
        # Missing critical configuration
        return "Server misconfiguration.", 500
    except MySQLdb.MySQLError:
        # Do not leak DB errors
        return "Database unavailable.", 503

    try:
        cursor = db.cursor()

        # 4) Check for existing username
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s",
            (username,)
        )
        if cursor.fetchone():
            return "Username already taken.", 409

        # 5) Insert the new user record
        cursor.execute(
            """
            INSERT INTO users
               (username, password_hash, salt, hash_iterations, created_at)
            VALUES
               (%s, %s, %s, %s, %s)
            """,
            (
                username,
                hash_hex,
                salt_hex,
                ITERATIONS,
                datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S')
            )
        )
        db.commit()

    except MySQLdb.MySQLError:
        # Roll back on any error
        db.rollback()
        # Log the exception server-side (omitted here)
        return "Registration failed.", 500

    finally:
        # Always clean up
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass

    # 6) On success, redirect to login page
    return redirect('/login')