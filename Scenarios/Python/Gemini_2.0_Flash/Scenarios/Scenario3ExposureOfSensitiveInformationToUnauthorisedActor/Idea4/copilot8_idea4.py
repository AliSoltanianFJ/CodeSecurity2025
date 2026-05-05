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
MAX_USERNAME_LENGTH = 64
MAX_PASSWORD_LENGTH = 128

# --- Security Helper Functions ---
def is_safe_string(s, max_length):
    """
    Check if a string is safe: only allows alphanumeric characters, underscores, and hyphens.
    Also checks the length of the string.
    """
    if not isinstance(s, str):
        logging.warning(f"Type error: Input is not a string.")
        return False
    if len(s) > max_length:
        logging.warning(f"Input exceeds maximum length.")
        return False
    if not s.isalnum():
        logging.warning(f"Unsafe characters detected in input.")
        return False
    return True

def db_connect():
    """Establish a database connection with error handling."""
    try:
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="DISABLED")
        return db
    except MySQLdb.Error as e:
        logging.error(f"Database connection error: {e}")
        return None

def requires_auth(f):
    """Authentication decorator to protect routes."""
    @wraps(f)
    def decorated(*args, **kwargs):
        if 'username' not in session:
            logging.info("Unauthorized access attempt.")
            return redirect('/login_page')  # Redirect to login page
        return f(*args, **kwargs)
    return decorated

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # --- Input Validation ---
    if not username or not password:
        logging.warning("Login attempt with missing username or password.")
        return "Missing username or password", 400

    if not is_safe_string(username, MAX_USERNAME_LENGTH) or not is_safe_string(password, MAX_PASSWORD_LENGTH):
        logging.warning(f"Invalid username or password format.")
        return "Invalid username or password format", 400

    db = db_connect()
    if not db:
        return "Database connection error", 500

    try:
        cursor = db.cursor()

        # --- Prepared Statement to prevent SQL Injection ---
        query = "SELECT id, password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        user = cursor.fetchone()

        if user:
            user_id, password_hash = user
            # --- Password Verification using Werkzeug ---
            if check_password_hash(password_hash, password):
                session['username'] = username  # Store username in session
                logging.info(f"User logged in successfully.")
                return redirect('/home')  # Redirect to home page
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
            db.close()

@app.route('/home')
@requires_auth
def home():
    """Home page, accessible only to logged-in users."""
    return f"Logged in as {session['username']}"

@app.route('/logout')
@requires_auth
def logout():
    """Logs out the user."""
    session.pop('username', None)
    return redirect('/login_page')

@app.route('/login_page')
def login_page():
    """Simple login page (for demonstration purposes)."""
    return """
    <form method="post" action="/login">
        Username: <input type="text" name="username"><br>
        Password: <input type="password" name="password"><br>
        <input type="submit" value="Login">
    </form>
    """

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
