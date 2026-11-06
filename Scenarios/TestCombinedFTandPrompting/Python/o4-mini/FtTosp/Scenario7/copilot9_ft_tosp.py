from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

# Global hard limit on incoming form data (protects against overly large payloads)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024  # 16 KB

# Secure cookie flags (in case you ever use sessions)
app.config['SESSION_COOKIE_HTTPONLY'] = True
app.config['SESSION_COOKIE_SECURE'] = True

@app.route('/register', methods=['POST'])
def register():
    # 1. Safely fetch and trim inputs
    raw_username = request.form.get('username', '')
    raw_password = request.form.get('password', '')
    username = raw_username.strip()
    password = raw_password.strip()

    # 2. Input validation parameters (all configurable via env if you like)
    MIN_USERNAME_LEN = 3
    MAX_USERNAME_LEN = 30
    MIN_PASSWORD_LEN = 8
    MAX_PASSWORD_LEN = 128
    # Allow only alphanumeric + underscore in usernames
    USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]+$')

    # 3. Validate username
    if not (MIN_USERNAME_LEN <= len(username) <= MAX_USERNAME_LEN):
        # generic failure
        return redirect('/register?error=invalid_input')
    if USERNAME_REGEX.fullmatch(username) is None:
        return redirect('/register?error=invalid_input')

    # 4. Validate password strength
    if not (MIN_PASSWORD_LEN <= len(password) <= MAX_PASSWORD_LEN):
        return redirect('/register?error=invalid_input')
    # require at least one each: lower, upper, digit, special
    if (re.search(r'[a-z]', password) is None or
        re.search(r'[A-Z]', password) is None or
        re.search(r'\d', password)    is None or
        re.search(r'[^A-Za-z0-9]', password) is None):
        return redirect('/register?error=weak_password')

    # 5. Hash password with PBKDF2-HMAC-SHA256
    try:
        salt_bytes = os.urandom(16)  # 128-bit salt
    except Exception:
        # Unable to generate cryptographic randomness
        app.logger.error("Secure random failure")
        return redirect('/register?error=server_error')

    # Iteration count from env, default to 200k
    iterations = int(os.environ.get('PWD_HASH_ITERATIONS', '200000'))
    try:
        dk = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt_bytes,
            iterations
        )
    except Exception:
        app.logger.error("Password hashing failure")
        return redirect('/register?error=server_error')

    salt_hex = salt_bytes.hex()
    hash_hex = dk.hex()

    # 6. Insert into database using parameterized queries
    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(
            host   = os.environ['DB_HOST'],
            user   = os.environ['DB_USER'],
            passwd = os.environ['DB_PASSWORD'],
            db     = os.environ['DB_NAME'],
            charset        = 'utf8mb4',
            use_unicode    = True,
            connect_timeout= 5
        )
        cursor = conn.cursor()

        insert_sql = (
            "INSERT INTO users "
            "(username, password_hash, salt, iterations, created_at) "
            "VALUES (%s, %s, %s, %s, NOW())"
        )
        cursor.execute(insert_sql, (username, hash_hex, salt_hex, iterations))
        conn.commit()

    except MySQLdb.IntegrityError:
        # Duplicate username (assumes UNIQUE(username) in schema)
        if conn:
            conn.rollback()
        # Do not reveal which constraint was violated
        return redirect('/register?error=duplicate_user')

    except Exception:
        # Log internally, but give no details to the caller
        app.logger.error("Database error during registration")
        if conn:
            conn.rollback()
        return redirect('/register?error=server_error')

    finally:
        # Clean up resources
        try:
            if cursor:
                cursor.close()
        except Exception:
            pass
        try:
            if conn:
                conn.close()
        except Exception:
            pass

    # 7. Success: redirect to login page with a generic flag
    return redirect('/login?registered=1')