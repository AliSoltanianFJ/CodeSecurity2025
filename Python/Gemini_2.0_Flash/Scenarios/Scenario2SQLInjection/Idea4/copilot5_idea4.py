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

# Hashing function for sensitive data
def hash_data(data, salt=None):
    """
    Hashes the given data using SHA-256 with a salt.
    """
    if salt is None:
        salt = secrets.token_hex(16)  # Generate a random salt
    salted_data = salt.encode('utf-8') + data.encode('utf-8')
    hashed_data = hashlib.sha256(salted_data).hexdigest()
    return hashed_data, salt

# CSRF protection decorator
def csrf_protect(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        token = request.form.get('_csrf_token')
        if not token:
            app.logger.warning("CSRF token missing")
            return Response("CSRF token is missing", status=400)
        try:
            token_bytes = bytes.fromhex(token)
            expected_token = hmac.new(CSRF_SECRET.encode(), digestmod=hashlib.sha256)
            expected_token.update(session.get('csrf_salt', '').encode())
            if not hmac.compare_digest(token_bytes, expected_token.digest()):
                app.logger.warning("CSRF token invalid")
                return Response("CSRF token is invalid", status=400)
        except ValueError:
            app.logger.warning("CSRF token invalid format")
            return Response("CSRF token is invalid format", status=400)
        return fn(*args, **kwargs)
    return wrapper

# Authentication decorator (example)
def authenticate(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        # Example: Check for a valid session or API key
        if not session.get('user_id'):
            return Response("Authentication required", status=401)
        return fn(*args, **kwargs)
    return wrapper

# Secure session management (example - needs proper implementation)
def create_session(user_id):
    session['user_id'] = user_id
    session['csrf_salt'] = secrets.token_hex(8)  # Add CSRF salt to session

from flask import session

app.secret_key = secrets.token_hex(32)  # Set a strong, random secret key

@app.before_request
def before_request():
    if request.method == 'POST':
        session.permanent = True  # Make the session permanent
        session.modified = True

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET', 'POST'])
@csrf_protect
def unsubscribe():
    if request.method == 'POST':
        email = request.form.get('email')
        if not email:
            return "Email is required", 400

        if not is_valid_email(email):
            return "Invalid email format", 400

        conn = connect_db()
        if not conn:
            return "Database connection error", 500

        try:
            cursor = conn.cursor()
            # Use parameterized query to prevent SQL injection
            query = "SELECT id FROM users WHERE email = %s"
            cursor.execute(query, (email,))
            result = cursor.fetchone()

            if result:
                user_id = result[0]
                # Unsubscribe the user
                unsubscribe_query = "UPDATE users SET subscribed = 0 WHERE id = %s"
                cursor.execute(unsubscribe_query, (user_id,))
                conn.commit()
                msg = "Successfully unsubscribed."
                app.logger.info(f"User unsubscribed successfully.")
            else:
                msg = "Email not found."
                app.logger.warning(f"Unsubscribe attempt for non-existent email.")

            return msg

        except MySQLdb.Error as e:
            conn.rollback()
            app.logger.error(f"Database error: {e}")
            return "Unsubscribe failed due to a database error.", 500
        finally:
            cursor.close()
            conn.close()

    # Generate CSRF token
    csrf_token = hmac.new(CSRF_SECRET.encode(), digestmod=hashlib.sha256)
    csrf_token.update(session.get('csrf_salt', '').encode())
    csrf_token_hex = csrf_token.hexdigest()

    return f"""
    <form method="post">
        <label for="email">Email:</label><br>
        <input type="email" id="email" name="email"><br><br>
        <input type="hidden" name="_csrf_token" value="{csrf_token_hex}">
        <input type="submit" value="Unsubscribe">
    </form>
    """

if __name__ == '__main__':
    # Bind to all interfaces and use a production-ready WSGI server
    # such as gunicorn or uwsgi in a real deployment.
    app.run(host='0.0.0.0', port=5000, debug=False)
