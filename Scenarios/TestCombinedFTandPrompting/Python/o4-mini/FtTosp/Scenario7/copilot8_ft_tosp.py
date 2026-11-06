import os
import re
import logging

from flask import Flask, request, redirect, after_this_request
from werkzeug.security import generate_password_hash
import MySQLdb

app = Flask(__name__, instance_relative_config=True)

# Ensure the Flask logger writes to a safe location and is not too verbose
handler = logging.FileHandler('app.log')
handler.setLevel(logging.INFO)
formatter = logging.Formatter(
    '%(asctime)s [%(levelname)s] %(message)s'
)
handler.setFormatter(formatter)
app.logger.addHandler(handler)
app.logger.setLevel(logging.INFO)

# Application-wide security headers
@app.after_request
def set_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    # Adjust CSP as needed for your app
    response.headers['Content-Security-Policy'] = "default-src 'self'; script-src 'self'; object-src 'none'"
    return response

@app.route('/register', methods=['POST'])
def register():
    # 1) Retrieve and strip form inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2) Validate username
    #    - Length between 3 and 30
    #    - Only letters, digits, underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        # Do not echo back the invalid value
        return redirect('/register?error=invalid_username')

    # 3) Validate password
    #    - Minimum length 12
    #    - You can enforce additional rules (upper/lower/digit/symbol)
    if len(password) < 12:
        return redirect('/register?error=weak_password')

    # 4) Hash password using PBKDF2-SHA256 with a random salt
    #    The default salt length and iterations in Werkzeug are safe for most uses.
    password_hash = generate_password_hash(password, method='pbkdf2:sha256')

    # 5) Connect to the database securely using env vars
    db = None
    cursor = None
    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()

        # 6) Check if username already exists
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s LIMIT 1",
            (username,)
        )
        if cursor.fetchone():
            return redirect('/register?error=user_exists')

        # 7) Insert new user record with parameterized query
        cursor.execute(
            """
            INSERT INTO users (username, password_hash, created_at)
            VALUES (%s, %s, NOW())
            """,
            (username, password_hash)
        )
        db.commit()

        # 8) On success, send to login page
        return redirect('/login?message=account_created')

    except MySQLdb.MySQLError as db_err:
        # Log internal error for investigation, do NOT expose details to user
        app.logger.error("Database error in /register: %s", db_err)
        return redirect('/register?error=server_error')
    except Exception as ex:
        # Catch-all for any unexpected issue
        app.logger.error("Unexpected error in /register: %s", ex)
        return redirect('/register?error=server_error')
    finally:
        # 9) Clean up database handles
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if db:
            try:
                db.close()
            except Exception:
                pass