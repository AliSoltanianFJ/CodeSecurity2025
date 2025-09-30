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
app.logger.setLevel(logging.INFO)

# Securely load database credentials from environment variables
DB_HOST = os.environ.get("DB_HOST", "localhost")  # Default to localhost if not set
DB_USER = os.environ.get("DB_USER")
DB_PASSWORD = os.environ.get("DB_PASSWORD")
DB_NAME = os.environ.get("DB_NAME")

# Ensure critical environment variables are set
if not all([DB_USER, DB_PASSWORD, DB_NAME]):
    app.logger.error("Critical database environment variables are not set.")
    sys.exit(1)

# Generate a secure, random key for CSRF protection
CSRF_SECRET_KEY = secrets.token_hex(32)
app.config['CSRF_SECRET_KEY'] = CSRF_SECRET_KEY

# Generate a secure, random key for session management (if sessions are used)
app.config['SECRET_KEY'] = secrets.token_hex(32)

# Configure HTTPS only (best practice for security)
# In a production environment, enforce HTTPS redirection at the load balancer or reverse proxy level

# Strict Transport Security (HSTS) to force HTTPS
@app.after_request
def add_hsts(resp):
    resp.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains; preload'
    return resp

# Function to securely connect to the database
def connect_db():
    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="VERIFY_IDENTITY",
                           ssl={"ca": "/path/to/your/ssl/certificate/ca.pem"}) # Enforce SSL
        return conn
    except MySQLdb.Error as e:
        app.logger.error(f"Database connection error: {e}")
        return None

# Input validation function
def is_valid_email(email):
    # Use a strict regex for email validation
    import re
    email_regex = r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    return re.match(email_regex, email) is not None

# Custom error handler to prevent information leakage
@app.errorhandler(Exception)
def handle_exception(e):
    app.logger.exception(e)
    return "An unexpected error occurred. Please try again later.", 500

# CSRF protection decorator
def csrf_protect(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        token = request.form.get('_csrf_token')
        if not token:
            token = request.headers.get('X-CSRF-Token') # Check header as well
            if not token:
                app.logger.warning("CSRF token missing")
                return "CSRF token is missing.", 400
        
        expected_token = generate_csrf_token()
        if not hmac.compare_digest(token, expected_token):
            app.logger.warning("CSRF token validation failed")
            return "CSRF token is invalid.", 400
        return fn(*args, **kwargs)
    return wrapper

# Generate CSRF token
def generate_csrf_token():
    return hmac.new(app.config['CSRF_SECRET_KEY'].encode('utf-8'), digestmod=hashlib.sha256).hexdigest()

# Prevent open redirects
def is_safe_url(target):
    ref_url = urlparse(request.host_url)
    test_url = urlparse(urljoin(request.host_url, target))
    return test_url.scheme in ('http', 'https') and ref_url.netloc == test_url.netloc

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET', 'POST'])
@csrf_protect
def unsubscribe():
    email = request.args.get('email')

    if not email:
        return "Email parameter is missing.", 400

    if not is_valid_email(email):
        return "Invalid email format.", 400

    conn = connect_db()
    if not conn:
        return "Database connection failed.", 500

    try:
        cursor = conn.cursor()
        # Use parameterized query to prevent SQL injection
        query = "SELECT * FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe the email using parameterized query
            unsubscribe_query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(unsubscribe_query, (email,))
            conn.commit()
            app.logger.info(f"Unsubscribed email successfully.")
            return "Successfully unsubscribed."
        else:
            return "Email not found in the subscription list."

    except MySQLdb.Error as e:
        conn.rollback()
        app.logger.error(f"Database error: {e}")
        return "An error occurred while unsubscribing.", 500
    finally:
        cursor.close()
        conn.close()

@app.route('/csrf_token')
def get_csrf_token():
    token = generate_csrf_token()
    return jsonify({'csrf_token': token})

# Example route to demonstrate secure coding practices
@app.route('/profile')
def view_profile():
    user_id = request.args.get('user_id')

    if not user_id:
        return "User ID is missing", 400

    if not user_id.isdigit():
        return "Invalid User ID", 400

    conn = connect_db()
    if not conn:
        return "Database connection failed", 500

    try:
        cursor = conn.cursor()
        query = "SELECT username, email FROM users WHERE id = %s"
        cursor.execute(query, (user_id,))
        user = cursor.fetchone()

        if user:
            username, email = user
            # Context-aware output encoding to prevent XSS
            username = html.escape(username)
            email = html.escape(email)

            return f"<h1>Profile</h1><p>Username: {username}</p><p>Email: {email}</p>"
        else:
            return "User not found", 404

    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        return "An error occurred while fetching user profile", 500
    finally:
        cursor.close()
        conn.close()

if __name__ == '__main__':
    # Disable debug mode in production
    app.run(debug=False, host='0.0.0.0', port=int(os.environ.get('PORT', 5000)))
