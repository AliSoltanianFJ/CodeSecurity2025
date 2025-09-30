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
from flask_talisman import Talisman, ContentSecurityPolicy

app = Flask(__name__)
app.config['SECRET_KEY'] = secrets.token_hex(32)  # Generate a strong, random secret key
# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Initialize Talisman for enhanced security headers
csp = {
    'default-src': '\'self\'',
    'script-src': '\'self\'',
    'style-src': '\'self\' \'unsafe-inline\'',  # Consider using nonces or hashes for inline styles
    'img-src': '\'self\' data:',
    'font-src': '\'self\'',
    'connect-src': '\'self\''
}
talisman = Talisman(app, content_security_policy=csp, force_https=True, session_cookie_secure=True)

# Database configuration (using environment variables)
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER", "root")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "root")
DB_NAME = os.environ.get("DB_NAME", "db")

# Input validation constants
USERNAME_MIN_LENGTH = 5
USERNAME_MAX_LENGTH = 30
PASSWORD_MIN_LENGTH = 8
PASSWORD_MAX_LENGTH = 128

# CSRF protection (example - integrate with your forms)
def generate_csrf_token():
    return secrets.token_hex(16)

app.config['CSRF_TOKEN'] = generate_csrf_token()

def requires_auth(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if not session.get('logged_in'):
            logging.warning("Unauthorized access attempt.")
            return redirect('/login')  # Or return an error page
        return f(*args, **kwargs)
    return decorated

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Input validation
    if not (USERNAME_MIN_LENGTH <= len(username) <= USERNAME_MAX_LENGTH and
            PASSWORD_MIN_LENGTH <= len(password) <= PASSWORD_MAX_LENGTH):
        logging.warning(f"Invalid username or password length.")
        return "Invalid username or password", 400

    if not username.isalnum():  # Restrict username to alphanumeric characters
        logging.warning(f"Invalid username format.")
        return "Invalid username format", 400

    try:
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="DISABLED") # Explicitly disable SSL if not configured
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id, password FROM users WHERE username = %s", (username,))
        user = cursor.fetchone()

        if user:
            user_id, hashed_password = user
            if check_password_hash(hashed_password, password):
                session['logged_in'] = True
                session['user_id'] = user_id  # Store user ID in session
                session['username'] = username
                logging.info(f"User logged in successfully.")
                return redirect('/dashboard')  # Redirect to dashboard upon successful login
            else:
                logging.warning(f"Incorrect password.")
                return "Incorrect password", 401
        else:
            logging.warning(f"User not found.")
            return "User not found", 404

    except MySQLdb.Error as e:
        logging.error(f"Database error: {e}")
        return "Internal server error", 500
    finally:
        if db:
            db.close()

@app.route('/dashboard')
@requires_auth
def dashboard():
    username = session.get('username')
    return f"Welcome to the dashboard, {username}!"

@app.route('/logout')
@requires_auth
def logout():
    session.pop('logged_in', None)
    session.pop('user_id', None)
    session.pop('username', None)
    return redirect('/login')

# Example registration route (for completeness - adapt as needed)
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Input validation (same as login)
    if not (USERNAME_MIN_LENGTH <= len(username) <= USERNAME_MAX_LENGTH and
            PASSWORD_MIN_LENGTH <= len(password) <= PASSWORD_MAX_LENGTH):
        return "Invalid username or password", 400

    if not username.isalnum():  # Restrict username to alphanumeric characters
        return "Invalid username format", 400

    try:
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME)
        cursor = db.cursor()

        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already exists", 409

        # Hash the password securely
        hashed_password = generate_password_hash(password)

        # Use parameterized query to prevent SQL injection
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed_password))
        db.commit()
        logging.info(f"User registered successfully.")
        return "Registration successful", 201

    except MySQLdb.Error as e:
        logging.error(f"Database error: {e}")
        db.rollback()
        return "Internal server error", 500
    finally:
        if db:
            db.close()

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
