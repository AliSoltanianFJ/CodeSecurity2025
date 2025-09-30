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
    'style-src': '\'self\'',
    'img-src': '\'self\' data:',
    'font-src': '\'self\'',
    'connect-src': '\'self\'',
    'object-src': '\'none\'',
    'base-uri': '\'none\'',
    'form-action': '\'self\''
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

# Custom error handler
@app.errorhandler(Exception)
def handle_exception(e):
    logging.exception("An exception occurred: %s", str(e))
    return "An unexpected error occurred. Please try again later.", 500

# Authentication decorator
def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if session.get('username') is None:
            return redirect('/login')
        return f(*args, **kwargs)
    return decorated_function

def is_safe_string(s):
    """
    Additional layer of input validation to prevent XSS.
    This function checks if the input string contains any potentially harmful characters or patterns.
    """
    if not isinstance(s, str):
        return False
    # Example: Check for script tags, event handlers, and other potentially harmful patterns
    if "<script" in s.lower() or "javascript:" in s.lower() or "onerror" in s.lower():
        return False
    # Add more checks as needed
    return True

def validate_username(username):
    """Validates the username against strict criteria."""
    if not isinstance(username, str):
        logging.warning("Username is not a string.")
        return False, "Username must be a string."
    if not (USERNAME_MIN_LENGTH <= len(username) <= USERNAME_MAX_LENGTH):
        logging.warning(f"Username length is invalid: {len(username)}.")
        return False, f"Username must be between {USERNAME_MIN_LENGTH} and {USERNAME_MAX_LENGTH} characters."
    if not username.isalnum():
        logging.warning("Username contains invalid characters.")
        return False, "Username must contain only alphanumeric characters."
    if not is_safe_string(username):
        logging.warning("Username contains potentially harmful characters.")
        return False, "Username contains potentially harmful characters."
    return True, None

def validate_password(password):
    """Validates the password against strict criteria."""
    if not isinstance(password, str):
        logging.warning("Password is not a string.")
        return False, "Password must be a string."
    if not (PASSWORD_MIN_LENGTH <= len(password) <= PASSWORD_MAX_LENGTH):
        logging.warning(f"Password length is invalid: {len(password)}.")
        return False, f"Password must be between {PASSWORD_MIN_LENGTH} and {PASSWORD_MAX_LENGTH} characters."
    # Enforce strong password policies (example: at least one uppercase, one lowercase, one digit, one special character)
    has_uppercase = any(c.isupper() for c in password)
    has_lowercase = any(c.islower() for c in password)
    has_digit = any(c.isdigit() for c in password)
    has_special = any(not c.isalnum() for c in password)

    if not (has_uppercase and has_lowercase and has_digit and has_special):
        logging.warning("Password does not meet complexity requirements.")
        return False, "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character."
    if not is_safe_string(password):
        logging.warning("Password contains potentially harmful characters.")
        return False, "Password contains potentially harmful characters."
    return True, None

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    try:
        username = request.form.get('username', '')
        password = request.form.get('password', '')

        # Input Validation
        username_valid, username_error = validate_username(username)
        password_valid, password_error = validate_password(password)

        if not username_valid:
            return f"Invalid username: {username_error}", 400
        if not password_valid:
            return f"Invalid password: {password_error}", 400

        # Database connection
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="DISABLED")
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        query = "SELECT id, password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        user = cursor.fetchone()

        if user:
            user_id, hashed_password = user
            if check_password_hash(hashed_password, password):
                # Authentication successful
                session['username'] = username  # Store username in session
                logging.info(f"User logged in successfully.")
                return redirect('/home')  # Redirect to home page
            else:
                logging.warning(f"Failed login attempt: incorrect password.")
                return "Invalid username or password", 401
        else:
            logging.warning(f"Failed login attempt: user not found.")
            return "Invalid username or password", 401

    except MySQLdb.Error as e:
        logging.error(f"Database error: {e}")
        return "Database error. Please try again later.", 500
    except Exception as e:
        logging.exception(f"An unexpected error occurred: {e}")
        return "An unexpected error occurred. Please try again later.", 500
    finally:
        if db:
            db.close()

@app.route('/home')
@login_required
def home():
    return f"Logged in as {session['username']}"

@app.route('/logout')
@login_required
def logout():
    session.pop('username', None)
    return redirect('/login')

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
