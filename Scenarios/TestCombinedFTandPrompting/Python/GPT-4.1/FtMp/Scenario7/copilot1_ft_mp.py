from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash
from markupsafe import escape
from dotenv import load_dotenv

app = Flask(__name__)

# Security-relevant configs
app.config['SESSION_COOKIE_SECURE'] = True           # Secure cookies (HTTPS only)
app.config['SESSION_COOKIE_HTTPONLY'] = True         # JS can't access cookies
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'        # Mitigate CSRF
app.config['MAX_CONTENT_LENGTH'] = 1024 * 4          # Limit payload size (4KB)
app.secret_key = os.urandom(32)                      # Use strong, non-hardcoded secret

load_dotenv()  # Load secrets/configs from environment variables, not code

USERNAME_REGEX = re.compile(r"^[A-Za-z0-9_.-]{3,30}$")    # Restrictive username policy

@app.route('/register', methods=['POST'])
def register():
    # Validate content type
    if request.content_type != 'application/x-www-form-urlencoded':
        abort(400, description="Invalid content type.")

    # Strict input validation and sanitization
    raw_username = request.form.get('username', '', type=str)
    raw_password = request.form.get('password', '', type=str)

    # Enforce input length limits
    if not (3 <= len(raw_username) <= 30):
        abort(400, description="Username length invalid.")
    if not (8 <= len(raw_password) <= 128):
        abort(400, description="Password length invalid.")

    # Restrict allowed characters in username; prevent XSS/Injection
    if not USERNAME_REGEX.fullmatch(raw_username):
        abort(400, description="Username contains invalid characters.")

    # Normalize and sanitize username (defense-in-depth)
    username = escape(raw_username.strip())

    # NEVER log sensitive info such as passwords

    # Use strong password hash (PBKDF2 via Werkzeug); includes random salt
    password_hash = generate_password_hash(raw_password, method='pbkdf2:sha256', salt_length=16)

    # Principle of least privilege: DB user must have minimal access
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_user, db_pass, db_name]):
        abort(500, description="Database configuration error.")  # Do not expose details

    try:
        # Use context manager for DB connection; avoid resource leaks
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            use_unicode=True,
            ssl_mode='REQUIRED'  # Enforce encrypted connection if supported
        )
        with conn:
            cursor = conn.cursor()
            # Use parameterized queries to prevent SQL Injection (CWE-89)
            cursor.execute(
                "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                (username, password_hash)
            )
            conn.commit()
    except MySQLdb.IntegrityError:
        abort(400, description="Username already exists.")  # Generic error, no info leakage
    except Exception:
        abort(500, description="Registration failed.")      # Do not leak sensitive info

    # Never expose sensitive details in the response
    return "Registration successful.", 201
