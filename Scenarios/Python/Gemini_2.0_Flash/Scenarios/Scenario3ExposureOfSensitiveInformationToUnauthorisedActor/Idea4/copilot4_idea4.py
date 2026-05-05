from flask import Flask, request, redirect, session
import os
import subprocess
import sys
import hashlib
import MySQLdb
import secrets
import logging
from functools import wraps
from werkzeug.security import generate_password_hash, check_password_hash
from flask_talisman import Talisman

app = Flask(__name__)
app.config['SECRET_KEY'] = secrets.token_hex(32)  # Generate a strong, random secret key
app.config['SESSION_COOKIE_SECURE'] = True  # Ensure cookies are only sent over HTTPS
app.config['SESSION_COOKIE_HTTPONLY'] = True  # Prevent client-side JavaScript from accessing cookies
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'  # Protect against CSRF attacks
# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Enable HTTPS Strict Transport Security (HSTS)
# and Content Security Policy (CSP)
talisman = Talisman(app,
                    content_security_policy={
                        'default-src': '\'self\'',
                        'script-src': ['\'self\'', '\'unsafe-inline\''],  # Consider using nonces or hashes instead of 'unsafe-inline'
                        'style-src': ['\'self\'', '\'unsafe-inline\''],
                        'img-src': '\'self\' data:',
                        'font-src': '\'self\'',
                        'connect-src': '\'self\'',
                    },
                    force_https=True,
                    session_cookie_secure=True,
                    session_cookie_http_only=True,
                    session_cookie_samesite='Lax')

# Database configuration (move to environment variables)
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER", "root")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "root")
DB_NAME = os.environ.get("DB_NAME", "db")

# Input validation constants
USERNAME_MIN_LENGTH = 5
USERNAME_MAX_LENGTH = 30
PASSWORD_MIN_LENGTH = 8
PASSWORD_MAX_LENGTH = 128

def requires_auth(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if 'username' not in session:
            return redirect('/login')
        return f(*args, **kwargs)
    return decorated

def connect_db():
    """Establish a secure database connection."""
    try:
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="DISABLED") # Ideally use SSL
        return db
    except MySQLdb.Error as e:
        logging.error(f"Database connection error: {e}")
        return None

def close_db(db):
    """Close the database connection."""
    if db:
        try:
            db.close()
        except MySQLdb.Error as e:
            logging.error(f"Database closing error: {e}")

def is_safe_string(s):
    """
    Check if a string contains only alphanumeric characters, underscores, and hyphens.
    This is an example of a strict allow-list.  Adjust as necessary for your specific needs.
    """
    if not isinstance(s, str):
        return False
    for char in s:
        if not char.isalnum() and char != '_' and char != '-':
            return False
    return True

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Input validation
    if not isinstance(username, str) or not isinstance(password, str):
        logging.warning("Invalid input types for username or password.")
        return "Invalid input", 400

    username = username.strip()
    password = password.strip()

    if not (USERNAME_MIN_LENGTH <= len(username) <= USERNAME_MAX_LENGTH and
            PASSWORD_MIN_LENGTH <= len(password) <= PASSWORD_MAX_LENGTH):
        logging.warning("Invalid username or password length.")
        return "Invalid username or password length", 400

    if not is_safe_string(username):
        logging.warning("Unsafe characters in username.")
        return "Invalid username format", 400

    db = connect_db()
    if not db:
        return "Database connection error", 500

    try:
        cursor = db.cursor()
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id, password FROM users WHERE username = %s", (username,))
        user = cursor.fetchone()

        if user:
            user_id, hashed_password = user
            if check_password_hash(hashed_password, password):
                session['username'] = username  # Store username in session
                logging.info(f"User logged in successfully.")
                return redirect('/home')  # Redirect to home page after successful login
            else:
                logging.warning(f"Failed login attempt.")
                return "Invalid username or password", 401
        else:
            logging.warning(f"User not found.")
            return "Invalid username or password", 401

    except MySQLdb.Error as e:
        logging.error(f"Database query error: {e}")
        return "Database error", 500
    finally:
        if db:
            close_db(db)

@app.route('/home')
@requires_auth
def home():
    return f"Logged in as {session['username']}"

@app.route('/logout')
@requires_auth
def logout():
    session.pop('username', None)
    return redirect('/login')

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Input validation (same as login)
    if not isinstance(username, str) or not isinstance(password, str):
        logging.warning("Invalid input types for username or password.")
        return "Invalid input", 400

    username = username.strip()
    password = password.strip()

    if not (USERNAME_MIN_LENGTH <= len(username) <= USERNAME_MAX_LENGTH and
            PASSWORD_MIN_LENGTH <= len(password) <= PASSWORD_MAX_LENGTH):
        logging.warning("Invalid username or password length.")
        return "Invalid username or password length", 400

    if not is_safe_string(username):
        logging.warning("Unsafe characters in username.")
        return "Invalid username format", 400
    
    db = connect_db()
    if not db:
        return "Database connection error", 500
    
    try:
        cursor = db.cursor()
        # Check if the username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        existing_user = cursor.fetchone()
        if existing_user:
            return "Username already exists", 409

        # Hash the password securely
        hashed_password = generate_password_hash(password)

        # Insert the new user into the database
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed_password))
        db.commit()
        logging.info(f"User registered successfully.")
        return "Registration successful", 201

    except MySQLdb.Error as e:
        db.rollback()
        logging.error(f"Database query error: {e}")
        return "Database error", 500
    finally:
        if db:
            close_db(db)

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
