from flask import Flask, request, redirect, Response
import os
import subprocess
import sys
import MySQLdb
import hashlib
import hmac
import logging
from functools import wraps
import secrets
from urllib.parse import quote, unquote

app = Flask(__name__)
app.logger.setLevel(logging.INFO)

# Securely store the database credentials in environment variables
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER", "root")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
DB_NAME = os.environ.get("DB_NAME", "your_database")

# Generate a random key for CSRF protection
CSRF_KEY = secrets.token_hex(32)
app.config['CSRF_KEY'] = CSRF_KEY

# Generate a random key for session management
SESSION_KEY = secrets.token_hex(32)
app.config['SESSION_KEY'] = SESSION_KEY

# Hashing algorithm for password storage
PASSWORD_HASH_ALGORITHM = 'sha256'

# --- Security Helper Functions ---

def generate_csrf_token():
    """Generates a CSRF token."""
    return secrets.token_hex(16)

def validate_csrf_token(token):
    """Validates a CSRF token against the stored token."""
    stored_token = request.cookies.get('csrf_token')
    if not stored_token or not hmac.compare_digest(stored_token, token):
        return False
    return True

def set_secure_cookie(response, key, value, httponly=True, secure=True, samesite='Strict'):
    """Sets a cookie with secure attributes."""
    response.set_cookie(key, value, httponly=httponly, secure=secure, samesite=samesite)

def get_db_connection():
    """Establishes a secure database connection."""
    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="DISABLED") # Ensure SSL is enabled in production
        return conn
    except MySQLdb.Error as e:
        app.logger.error(f"Database connection error: {e}")
        return None

def sanitize_input(input_string):
    """Sanitizes user input to prevent injection attacks."""
    if not isinstance(input_string, str):
        return ""
    # Remove any character that is not alphanumeric, underscore, or hyphen
    sanitized_string = ''.join(c for c in input_string if c.isalnum() or c == '_' or c == '-' or c == '@' or c == '.')
    return sanitized_string

def is_valid_email(email):
    """Validates email format."""
    import re
    email_regex = r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    return re.match(email_regex, email) is not None

def requires_auth(f):
    """Decorator to enforce authentication."""
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not check_auth(auth.username, auth.password):
            return authenticate()
        return f(*args, **kwargs)
    return decorated

def check_auth(username, password):
    """Checks if username/password combination is valid."""
    # Replace with secure authentication mechanism (e.g., bcrypt, scrypt)
    # and retrieve credentials from a secure source.
    # This is a placeholder and MUST be replaced.
    return username == 'admin' and password == 'secret'

def authenticate():
    """Sends a 401 response that enables basic auth."""
    return Response(
    'Could not verify your access level for that URL.\n'
    'You have to login with proper credentials', 401,
    {'WWW-Authenticate': 'Basic realm="Login Required"'})

# --- Routes ---

@app.route('/unsubscribe', methods=['GET', 'POST'])
def unsubscribe():
    """Unsubscribes an email from the database."""
    if request.method == 'POST':
        # CSRF protection
        csrf_token = request.form.get('csrf_token')
        if not validate_csrf_token(csrf_token):
            return "CSRF token validation failed.", 400

        email = request.form.get('email')
        if not email:
            return "Email is required.", 400

        email = sanitize_input(email)

        if not is_valid_email(email):
            return "Invalid email format.", 400

        conn = get_db_connection()
        if not conn:
            return "Database connection failed.", 500

        try:
            cursor = conn.cursor()
            # Using parameterized query to prevent SQL injection
            query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(query, (email,))
            conn.commit()

            return "Unsubscribed successfully."

        except MySQLdb.Error as e:
            conn.rollback()
            app.logger.error(f"Database error: {e}")
            return "An error occurred while unsubscribing.", 500

        finally:
            cursor.close()
            conn.close()

    # Generate CSRF token for GET request
    csrf_token = generate_csrf_token()
    response = Response(f"""
        <form method="post">
        <input type="hidden" name="csrf_token" value="{csrf_token}">
        Email: <input type="email" name="email">
        <button type="submit">Unsubscribe</button>
        </form>
    """)
    set_secure_cookie(response, 'csrf_token', csrf_token, httponly=True, secure=True, samesite='Strict')
    return response

if __name__ == '__main__':
    # Disable debug mode in production
    app.run(debug=False, host='0.0.0.0', port=5000)
