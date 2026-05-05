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
    'default-src': ['\'self\''],
    'script-src': ['\'self\'', '\'unsafe-inline\''],  # Consider using nonces or hashes instead of 'unsafe-inline'
    'style-src': ['\'self\'', '\'unsafe-inline\''],
    'img-src': ['\'self\'', 'data:'],
    'font-src': ['\'self\''],
    'connect-src': ['\'self\''],
}
talisman = Talisman(app, content_security_policy=csp, force_https=True, session_cookie_secure=True)

# Database configuration (move to environment variables)
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER", "root")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "root")
DB_NAME = os.environ.get("DB_NAME", "db")

# Input validation constants
MAX_USERNAME_LENGTH = 50
MAX_PASSWORD_LENGTH = 100

# --- Security Helper Functions ---
def is_safe_string(s, max_length):
    """
    Check if a string is safe: only contains alphanumeric characters, underscores, and hyphens, and is within the length limit.
    """
    if not isinstance(s, str):
        logging.warning(f"Type error: Input is not a string.")
        return False
    if len(s) > max_length:
        logging.warning(f"Length error: Input exceeds maximum length.")
        return False
    if not s.isalnum():
        logging.warning(f"Alphanumeric error: Input contains invalid characters.")
        return False
    return True

def sanitize_input(input_string):
    """Sanitize input string to prevent injection attacks."""
    if not isinstance(input_string, str):
        logging.error(f"Type error: Input is not a string.")
        return ""
    # Remove any character that is not alphanumeric, underscore, or hyphen
    sanitized_string = ''.join(c for c in input_string if c.isalnum() or c == '_' or c == '-')
    return sanitized_string

def requires_auth(f):
    """Decorator to require authentication for a route."""
    @wraps(f)
    def decorated(*args, **kwargs):
        if 'username' not in session:
            logging.info("Authentication required: redirecting to login")
            return redirect('/login')
        return f(*args, **kwargs)
    return decorated

# --- Authentication ---
"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Input validation and sanitization
    if not username or not password:
        logging.warning("Login failed: Username or password missing")
        return "Username and password are required", 400

    if not is_safe_string(username, MAX_USERNAME_LENGTH) or not is_safe_string(password, MAX_PASSWORD_LENGTH):
        logging.warning(f"Login failed: Invalid username or password format.")
        return "Invalid username or password format", 400

    sanitized_username = sanitize_input(username)

    try:
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="DISABLED")
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id, password FROM users WHERE username = %s", (sanitized_username,))
        user = cursor.fetchone()

        if user:
            user_id, hashed_password = user
            if check_password_hash(hashed_password, password):
                session['username'] = sanitized_username
                session['user_id'] = user_id  # Store user ID in session
                logging.info(f"User logged in successfully")
                return redirect('/home')
            else:
                logging.warning(f"Login failed: Incorrect password.")
                return "Incorrect password", 401
        else:
            logging.warning(f"Login failed: User {sanitized_username} not found")
            return "User not found", 404

    except MySQLdb.Error as e:
        logging.error(f"Database error: {e}")
        return "Internal server error", 500
    finally:
        if db:
            db.close()

@app.route('/logout')
@requires_auth
def logout():
    """Logout the user."""
    username = session.get('username')
    session.pop('username', None)
    session.pop('user_id', None)
    logging.info(f"User {username} logged out")
    return redirect('/')

@app.route('/home')
@requires_auth
def home():
    """Home page for logged-in users."""
    username = session['username']
    return f"Logged in as {username}"

@app.route('/')
def index():
    """Index page."""
    return "Hello, please log in"

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
