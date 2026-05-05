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
HMAC_KEY = os.environ.get("HMAC_KEY", secrets.token_hex(32)).encode('utf-8')

# CSRF protection secret
CSRF_SECRET = os.environ.get("CSRF_SECRET", secrets.token_hex(32))

# Configure logging
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(levelname)s - %(message)s')

# --- Security Helper Functions ---

def generate_csrf_token():
    """Generates a CSRF token."""
    return secrets.token_hex(32)

def verify_csrf_token(token):
    """Verifies the CSRF token."""
    return hmac.compare_digest(token, session.get('csrf_token'))

def requires_auth(f):
    """Decorator to enforce authentication."""
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not authenticate(auth.username, auth.password):
            return authenticate_response()
        return f(*args, **kwargs)
    return decorated

def authenticate(username, password):
    """Dummy authentication function (replace with a secure method)."""
    # Replace with secure authentication (e.g., bcrypt, JWT)
    return username == 'admin' and password == 'secret'

def authenticate_response():
    """Sends a 401 response that enables basic auth."""
    return Response(
    'Could not verify your access!', 401,
    {'WWW-Authenticate': 'Basic realm="Login Required"'})

def sanitize_input(input_string):
    """Sanitizes input to prevent XSS and other injection attacks."""
    if not isinstance(input_string, str):
        return ''
    # Escape HTML entities
    input_string = input_string.replace('&', '&amp;')
    input_string = input_string.replace('<', '&lt;')
    input_string = input_string.replace('>', '&gt;')
    input_string = input_string.replace('"', '&quot;')
    input_string = input_string.replace("'", '&#39;')
    return input_string

def hash_email(email):
    """Hashes the email address using SHA-256 for anonymity."""
    if not isinstance(email, str):
        return None
    hashed_email = hashlib.sha256(email.encode('utf-8')).hexdigest()
    return hashed_email

def is_valid_email(email):
    """Validates email format using a regular expression."""
    import re
    email_regex = r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    return re.match(email_regex, email) is not None

def create_db_connection():
    """Creates a secure database connection."""
    try:
        conn = MySQLdb.connect(
            host=DB_HOST,
            user=DB_USER,
            passwd=DB_PASSWORD,
            db=DB_NAME,
            ssl_mode="DISABLED",  # Ensure SSL is enabled in production
            charset="utf8mb4",
            use_unicode=True
        )
        return conn
    except MySQLdb.Error as e:
        app.logger.error(f"Database connection error: {e}")
        return None

def close_db_connection(conn):
    """Closes the database connection."""
    if conn:
        try:
            conn.close()
        except MySQLdb.Error as e:
            app.logger.error(f"Error closing database connection: {e}")

def execute_db_query(conn, query, params=None):
    """Executes a database query with proper error handling and logging."""
    cursor = None
    try:
        cursor = conn.cursor()
        app.logger.info(f"Executing query: {query} with params: {params}")
        cursor.execute(query, params)
        conn.commit()
        return cursor
    except MySQLdb.Error as e:
        conn.rollback()
        app.logger.error(f"Database query error: {e}")
        return None
    finally:
        if cursor:
            try:
                cursor.close()
            except MySQLdb.Error as e:
                app.logger.error(f"Error closing cursor: {e}")

# --- Application Routes ---

@app.route('/unsubscribe', methods=['GET', 'POST'])
def unsubscribe():
    """Unsubscribes an email from the database."""
    if request.method == 'POST':
        # CSRF Protection
        csrf_token = request.form.get('csrf_token')
        if not csrf_token or not verify_csrf_token(csrf_token):
            return "CSRF token is invalid.", 400

        email = request.form.get('email')
        if not email:
            return "Email is required.", 400

        if not is_valid_email(email):
            return "Invalid email format.", 400

        hashed_email = hash_email(email)
        conn = create_db_connection()
        if not conn:
            return "Database connection failed.", 500

        try:
            # Use a parameterized query to prevent SQL injection
            query = "DELETE FROM subscribers WHERE hashed_email = %s"
            cursor = execute_db_query(conn, query, (hashed_email,))

            if cursor and cursor.rowcount > 0:
                return "Successfully unsubscribed!"
            else:
                return "Email not found in the database.", 404
        finally:
            close_db_connection(conn)

    # Generate CSRF token for GET requests
    csrf_token = generate_csrf_token()
    session['csrf_token'] = csrf_token

    # Display unsubscribe form with CSRF token
    return f"""
    <form method="post">
        <label for="email">Email:</label><br>
        <input type="email" id="email" name="email"><br><br>
        <input type="hidden" name="csrf_token" value="{csrf_token}">
        <input type="submit" value="Unsubscribe">
    </form>
    """

if __name__ == '__main__':
    # Use a secure WSGI server in production (e.g., Gunicorn, uWSGI)
    # Ensure HTTPS is enabled with a valid SSL certificate
    app.secret_key = os.environ.get("FLASK_SECRET_KEY", secrets.token_hex(24))
    app.run(debug=False, host='0.0.0.0', port=int(os.environ.get('PORT', 5000)))
