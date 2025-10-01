
from flask import Flask, request, redirect, abort
import re
import os
import MySQLdb
from passlib.hash import argon2

app = Flask(__name__)

# --- Secure Configuration ---
app.config.update(
    DEBUG=False,                                 # disable debug mode in production
    SESSION_COOKIE_HTTPONLY=True,                # mitigate XSS
    SESSION_COOKIE_SAMESITE='Lax',               # CSRF protection
    SESSION_COOKIE_SECURE=True                   # require HTTPS
)

# Register endpoint
@app.route('/register', methods=['POST'])
def register():
    # 1. Input Validation & Canonicalization
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Whitelist username: 3–30 chars, alphanumeric + underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400, 'Invalid username format')

    # Enforce strong password policy
    if len(password) < 12:
        abort(400, 'Password must be at least 12 characters')
    if not re.search(r'[A-Z]', password):
        abort(400, 'Password must include an uppercase letter')
    if not re.search(r'[a-z]', password):
        abort(400, 'Password must include a lowercase letter')
    if not re.search(r'\d', password):
        abort(400, 'Password must include a digit')
    if not re.search(r'[^\w]', password):
        abort(400, 'Password must include a special character')

    # 2. Password Hashing (Argon2 with recommended parameters)
    password_hash = argon2.using(
        time_cost=3,         # number of iterations
        memory_cost=64 * 1024,  # 64 MiB
        parallelism=2
    ).hash(password)

    # 3. Database Interaction (least-privilege user, parameterized queries)
    conn = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'app_user'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME', 'app_db'),
        charset='utf8mb4',
        use_unicode=True
    )

    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                (username, password_hash)
            )
        conn.commit()
    except MySQLdb.IntegrityError:
        conn.rollback()
        # Username uniqueness violation
        abort(409, 'Username already exists')
    finally:
        conn.close()

    # 4. Safe Redirect (avoid open-redirect issues)
    return redirect('/login', code=302)