import os
import re
import logging
from flask import Flask, request, render_template, redirect, flash, abort
from werkzeug.security import generate_password_hash
import MySQLdb

app = Flask(__name__)

# SECRET_KEY for session/flash – must be set in the environment
app.secret_key = os.environ.get('SECRET_KEY') or os.urandom(32)

# Database connection parameters from environment
DB_CONFIG = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'user': os.environ.get('DB_USER'),
    'passwd': os.environ.get('DB_PASS'),
    'db': os.environ.get('DB_NAME'),
    'charset': 'utf8mb4',
    # If you have an SSL CA certificate, set DB_SSL_CA to its path; otherwise omit ssl
    'ssl': {'ca': os.environ.get('DB_SSL_CA')} if os.environ.get('DB_SSL_CA') else None
}

# Compile once at startup
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,30}$')
# At least one lowercase, uppercase, digit, and special char, length 8–128
PASSWORD_REGEX = re.compile(
    r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,128}$'
)

def get_db_connection():
    """
    Returns a new MySQL db connection using SSL if configured.
    """
    # Filter out None values (ssl may be None)
    cfg = {k: v for k, v in DB_CONFIG.items() if v is not None}
    return MySQLdb.connect(**cfg)

@app.route('/register', methods=['GET'])
def register_form():
    """
    Render the registration form. The template must escape all user data.
    """
    return render_template('register.html')

@app.route('/register', methods=['POST'])
def register():
    # 1) Retrieve and strip input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # 2) Basic presence check
    if not username or not password:
        flash('Username and password are required.')
        return render_template('register.html'), 400

    # 3) Length limits to prevent excessive memory use
    if len(username) > 30 or len(password) > 128:
        flash('Input exceeds maximum allowed length.')
        return render_template('register.html'), 400

    # 4) Format validation
    if not USERNAME_REGEX.fullmatch(username):
        flash('Username must be 3–30 chars, letters, numbers, or underscores only.')
        return render_template('register.html'), 400

    if not PASSWORD_REGEX.fullmatch(password):
        flash('Password must be 8–128 chars and include uppercase, lowercase, '
              'digit, and special character.')
        return render_template('register.html'), 400

    # 5) Hash password with a strong, salted algorithm
    #    werkzeug.security.generate_password_hash uses PBKDF2‐SHA256 by default
    password_hash = generate_password_hash(
        password,
        method='pbkdf2:sha256',
        salt_length=16
    )

    # 6) Insert into the database using parameterized queries only
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 6a) Check for existing username
        cursor.execute('SELECT id FROM users WHERE username = %s LIMIT 1', (username,))
        if cursor.fetchone():
            flash('That username is already taken.')
            return render_template('register.html'), 409

        # 6b) Insert new user
        cursor.execute(
            'INSERT INTO users (username, password_hash) VALUES (%s, %s)',
            (username, password_hash)
        )
        conn.commit()

    except MySQLdb.Error as err:
        # Log error internally, but do not reveal details to the user
        app.logger.error('Database error during registration: %s', err)
        # Generic error page – do not leak stack trace or DB info
        return render_template('error.html'), 500

    finally:
        # Clean up resources
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

    # 7) On success, redirect to login
    flash('Registration successful. Please log in.')
    return redirect('/login')