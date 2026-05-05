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
from urllib.parse import urlparse, urljoin

app = Flask(__name__)
app.config['SECRET_KEY'] = secrets.token_hex(32)  # Generate a strong, random secret key

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Database configuration loaded from environment variables
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER", "root")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
DB_NAME = os.environ.get("DB_NAME", "your_database")

# Strict transport security header
@app.after_request
def add_security_headers(response):
    response.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains; preload'
    response.headers['X-Frame-Options'] = 'SAMEORIGIN'
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    response.headers['Content-Security-Policy'] = "default-src 'self'"
    response.headers['Referrer-Policy'] = 'strict-origin-when-cross-origin'
    return response

# Custom error handler to prevent information leakage
@app.errorhandler(Exception)
def handle_exception(e):
    logging.exception("An unhandled exception occurred: %s", str(e))
    return "An unexpected error occurred. Please contact support.", 500

# Input validation decorator
def validate_input(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        email = request.args.get('email', '')
        if not is_valid_email(email):
            logging.warning(f"Invalid email format.")
            return "Invalid email address", 400
        return f(*args, **kwargs, email=email)
    return decorated_function

# Email validation function
def is_valid_email(email):
    # Use a strict regex for email validation
    import re
    email_regex = r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    return re.match(email_regex, email) is not None

# Anti-CSRF token generation and validation
def generate_csrf_token():
    return secrets.token_hex(16)

def validate_csrf_token(token):
    session_token = request.cookies.get('csrf_token')
    if not session_token or not hmac.compare_digest(token, session_token):
        return False
    return True

# Database connection function
def get_db_connection():
    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="VERIFY_IDENTITY",
                           ssl={"ca": "/path/to/your/ssl/certificate"}) # Enforce SSL
        return conn
    except MySQLdb.Error as e:
        logging.error(f"Database connection error: {e}")
        raise

# Secure database interaction
@validate_input
@app.route('/unsubscribe')
def unsubscribe(email):
    csrf_token = request.args.get('csrf_token')
    if not validate_csrf_token(csrf_token):
        logging.warning("CSRF token validation failed")
        return "CSRF token is invalid", 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Use parameterized query to prevent SQL injection
        query = "SELECT * FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe the email using a parameterized query
            delete_query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(delete_query, (email,))
            conn.commit()
            logging.info(f"Unsubscribed email: {email}")
            return "Unsubscribed successfully"
        else:
            logging.info(f"Email not found: {email}")
            return "Email not found in our subscription list."

    except MySQLdb.Error as e:
        conn.rollback()
        logging.error(f"Database error: {e}")
        return "An error occurred while unsubscribing. Please try again later.", 500
    except Exception as e:
        logging.error(f"Unexpected error: {e}")
        return "An unexpected error occurred. Please try again later.", 500
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

# Generate a new CSRF token for the session
@app.route('/get_csrf_token')
def get_csrf_token_route():
    token = generate_csrf_token()
    resp = Response(json.dumps({'csrf_token': token}), mimetype='application/json')
    resp.set_cookie('csrf_token', token, httponly=True, secure=True, samesite='Strict')  # Secure cookie
    return resp

# Validate redirect URLs
def is_safe_url(target):
    ref_url = urlparse(request.host_url)
    test_url = urlparse(urljoin(request.host_url, target))
    return test_url.scheme in ('http', 'https') and ref_url.netloc == test_url.netloc

@app.route('/redirect')
def redirect_route():
    next_url = request.args.get('next')
    if next_url and is_safe_url(next_url):
        return redirect(next_url)
    else:
        return "Invalid redirect URL", 400

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=int(os.environ.get('PORT', 5000)), ssl_context='adhoc')
