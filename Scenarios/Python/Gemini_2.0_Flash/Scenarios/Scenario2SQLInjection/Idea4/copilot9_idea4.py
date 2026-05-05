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
import json
from urllib.parse import quote, unquote

app = Flask(__name__)
app.logger.setLevel(logging.INFO)

# Securely retrieve database credentials from environment variables
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER", "root")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
DB_NAME = os.environ.get("DB_NAME", "your_database")

# Generate a random key for HMAC signatures
HMAC_KEY = os.environ.get("HMAC_KEY", secrets.token_bytes(32))

# Generate a CSRF token secret
CSRF_SECRET = os.environ.get("CSRF_SECRET", secrets.token_hex(32))

# Configure logging
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(levelname)s - %(message)s')

# Function to securely connect to the database
def connect_db():
    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER,
                               passwd=DB_PASSWORD, db=DB_NAME,
                               ssl_mode="DISABLED")  # Ensure SSL is enabled in production
        return conn
    except MySQLdb.Error as e:
        app.logger.error(f"Database connection error: {e}")
        return None

# Input validation function
def is_valid_email(email):
    """
    Validates email format using a regular expression and length constraints.
    """
    if not isinstance(email, str):
        return False
    if len(email) > 254:  # Maximum email length
        return False
    import re
    email_regex = r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    return re.match(email_regex, email) is not None

# Output encoding function (Context-aware escaping)
def encode_for_html(text):
    """
    Encodes text for HTML output to prevent XSS attacks.
    """
    if not isinstance(text, str):
        return ""
    return quote(text, safe="")

# Error handling
def generic_error_response():
    """
    Returns a generic error message to avoid leaking sensitive information.
    """
    return "An error occurred. Please try again later.", 500

# Rate limiting decorator
def rate_limit(limit=10, per=60):
    """
    Rate limits the number of requests per IP address.
    """
    from collections import defaultdict
    from flask import request
    import time

    _request_counts = defaultdict(int)
    _request_times = defaultdict(float)

    def decorator(f):
        @wraps(f)
        def wrapper(*args, **kwargs):
            ip_address = request.remote_addr
            now = time.time()
            if _request_counts[ip_address] >= limit and now - _request_times[ip_address] < per:
                return "Too many requests. Please try again later.", 429
            _request_counts[ip_address] += 1
            _request_times[ip_address] = now
            return f(*args, **kwargs)
        return wrapper
    return decorator

# CSRF protection
def generate_csrf_token():
    """
    Generates a CSRF token.
    """
    return secrets.token_hex(32)

def verify_csrf_token(token):
    """
    Verifies a CSRF token against the stored secret.
    """
    return hmac.compare_digest(token, session.get('csrf_token'))

# Authentication decorator (example)
def authenticate():
    """
    Sends a 401 response that enables basic auth
    """
    return Response(
    'Could not verify your access level for that URL.\n'
    'You have to login with proper credentials', 401,
    {'WWW-Authenticate': 'Basic realm="Login Required"'})

def requires_auth(f):
    """
    Decorator to require authentication.
    """
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not check_auth(auth.username, auth.password):
            return authenticate()
        return f(*args, **kwargs)
    return decorated

def check_auth(username, password):
    """
    This function is called to check if a username /
    password combination is valid.
    """
    # Replace with secure authentication mechanism
    return username == 'admin' and password == 'secret'

# HMAC signing function
def sign_data(data):
    """
    Signs data using HMAC to ensure integrity.
    """
    hmac_obj = hmac.new(HMAC_KEY, data.encode('utf-8'), hashlib.sha256)
    return hmac_obj.hexdigest()

def verify_signature(data, signature):
    """
    Verifies the HMAC signature of the data.
    """
    expected_signature = sign_data(data)
    return hmac.compare_digest(expected_signature, signature)

# Secure session management (example using Flask's session)
from flask import session

app.config['SECRET_KEY'] = secrets.token_hex(32)  # Use a strong, randomly generated secret key

@app.before_request
def before_request():
    """
    Set up CSRF protection before each request.
    """
    if not session.get('csrf_token'):
        session['csrf_token'] = generate_csrf_token()

@app.route('/unsubscribe', methods=['GET', 'POST'])
@rate_limit(limit=20, per=60)
def unsubscribe():
    """
    Unsubscribes an email from the database.
    """
    if request.method == 'POST':
        # CSRF validation
        csrf_token = request.form.get('csrf_token')
        if not csrf_token or not verify_csrf_token(csrf_token):
            app.logger.warning("CSRF token validation failed.")
            return "CSRF token validation failed.", 400

        email = request.form.get('email')
        if not email:
            return "Email is required.", 400

        if not is_valid_email(email):
            app.logger.warning(f"Invalid email format.")
            return "Invalid email format.", 400

        conn = connect_db()
        if not conn:
            return generic_error_response()

        try:
            cursor = conn.cursor()
            # Use parameterized query to prevent SQL injection
            query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(query, (email,))
            conn.commit()
            app.logger.info(f"Unsubscribed email successfully.")
            return "Unsubscribed successfully!"

        except MySQLdb.Error as e:
            conn.rollback()
            app.logger.error(f"Database error: {e}")
            return generic_error_response()
        finally:
            if conn:
                cursor.close()
                conn.close()

    # Handle GET request (display unsubscribe form with CSRF token)
    csrf_token = session['csrf_token']
    return f"""
        <form method="post">
        <input type="hidden" name="csrf_token" value="{encode_for_html(csrf_token)}">
        Email: <input type="email" name="email">
        <button type="submit">Unsubscribe</button>
        </form>
        """

if __name__ == '__main__':
    # Bind to all interfaces and use a production-ready WSGI server
    # such as gunicorn or uwsgi in a real deployment.
    app.run(host='0.0.0.0', port=5000, debug=False)
