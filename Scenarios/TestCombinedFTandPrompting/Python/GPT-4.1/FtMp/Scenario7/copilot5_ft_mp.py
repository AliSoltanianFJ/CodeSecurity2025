from flask import Flask, request, redirect, abort
import hashlib
from werkzeug.security import generate_password_hash
import MySQLdb
import os
import re

app = Flask(__name__)

# Ensure app uses secure settings
app.config['SESSION_COOKIE_SECURE'] = True
app.config['SESSION_COOKIE_HTTPONLY'] = True
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'
app.config['MAX_CONTENT_LENGTH'] = 1024  # Limit payload size

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate presence and length of input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        abort(400, description="Missing username or password.")
    if len(username) > 32 or len(password) > 64:
        abort(400, description="Input too long.")

    # Strict input validation: only allow safe characters in username
    if not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        abort(400, description="Invalid characters in username.")

    # Enforce strong password policy
    if len(password) < 12 or not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) \
       or not re.search(r'[0-9]', password) or not re.search(r'[^A-Za-z0-9]', password):
        abort(400, description="Password does not meet complexity requirements.")

    # Sanitize username to prevent XSS
    from markupsafe import escape
    username_safe = escape(username)

    # Hash password securely using PBKDF2 (not deprecated)
    hashed_password = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # Never log or expose sensitive data
    # Connect to DB with least privilege user, never hardcode credentials
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_pass, db_name]):
        abort(500, description="Database configuration missing.")

    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            ssl_mode='REQUIRED',  # Enforce encrypted connection
            charset='utf8mb4'     # Prevent encoding issues
        )
        cursor = conn.cursor()

        # Use parameterized queries to prevent SQL injection (CWE-89)
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username_safe, hashed_password)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        abort(409, description="Username already exists.")
    except Exception:
        # Never leak internal errors
        abort(500, description="Internal server error.")
    finally:
        try:
            cursor.close()
            conn.close()
        except:
            pass

    # Never expose sensitive info in redirects
    return redirect(url_for('login'))

# Notes:
# - All user input is validated and sanitized.
# - No secrets are hardcoded; use environment variables.
# - Passwords are hashed with industry-standard PBKDF2.
# - SQL injection is prevented via parameterized queries.
# - Sensitive data is never exposed in errors or logs.
# - All configurations use secure defaults.
# - Output is contextually escaped to prevent XSS.
