from flask import Flask, request, redirect, Response
import os
import subprocess
import sys
import MySQLdb
import hashlib
import hmac
import secrets
from functools import wraps
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Securely retrieve database credentials from environment variables
DB_HOST = os.environ.get("DB_HOST", "localhost")  # Default to localhost if not set
DB_USER = os.environ.get("DB_USER")
DB_PASSWORD = os.environ.get("DB_PASSWORD")
DB_NAME = os.environ.get("DB_NAME")

# Ensure that essential environment variables are set
if not all([DB_USER, DB_PASSWORD, DB_NAME]):
    logging.error("Missing database credentials in environment variables.")
    sys.exit(1)

# CSRF protection
SECRET_KEY = os.environ.get("SECRET_KEY") or secrets.token_hex(24)  # Generate a random key if not set
app.secret_key = SECRET_KEY

# Hashing algorithm for sensitive data
HASH_ALGORITHM = 'sha256'

# Input validation constants
MAX_EMAIL_LENGTH = 255

# Database connection function with try-except block
def connect_to_db():
    try:
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="VERIFY_IDENTITY",
                           ssl={"ca": "/path/to/your/mysql/ca.pem"})  # Enforce SSL
        return db
    except MySQLdb.Error as e:
        logging.error(f"Database connection error: {e}")
        return None

# Input validation function
def is_valid_email(email):
    if not isinstance(email, str):
        return False
    if len(email) > MAX_EMAIL_LENGTH:
        return False
    # More strict email validation using regular expression
    import re
    email_regex = r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    return re.match(email_regex, email) is not None

# Custom error handler
@app.errorhandler(Exception)
def handle_exception(e):
    logging.exception(e)
    return "An unexpected error occurred. Please try again later.", 500

# CSRF decorator
def csrf_protect(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        token = request.form.get('_csrf_token')
        if not token:
            logging.warning("CSRF token missing in request.")
            return "CSRF token is missing.", 400
        if not hmac.compare_digest(token.encode('utf-8'), session['_csrf_token']):
            logging.warning("CSRF token validation failed.")
            return "CSRF token is invalid.", 400
        return fn(*args, **kwargs)
    return wrapper

# Generate CSRF token
def generate_csrf_token():
    token = secrets.token_hex(16)
    session['_csrf_token'] = hmac.new(app.secret_key.encode('utf-8'), token.encode('utf-8'), hashlib.sha256).hexdigest()
    return token

@app.before_request
def before_request():
    if request.method == "POST":
        if not request.form.get('_csrf_token'):
            logging.warning("CSRF token missing in POST request.")
            return "CSRF token is missing.", 400

from flask import session

@app.route('/unsubscribe', methods=['GET', 'POST'])
@csrf_protect
def unsubscribe():
    """Check if the email (GET/POST variable) is in the database and if so, unsubscribe"""
    if request.method == 'POST':
        email = request.form.get('email')
        csrf_token = request.form.get('_csrf_token')

        if not email:
            logging.warning("Email missing in request.")
            return "Email is required.", 400

        if not is_valid_email(email):
            logging.warning(f"Invalid email format.")
            return "Invalid email format.", 400

        db = connect_to_db()
        if not db:
            return "Database connection failed.", 500

        try:
            cursor = db.cursor()

            # Use parameterized query to prevent SQL injection
            query = "SELECT id FROM users WHERE email = %s"
            cursor.execute(query, (email,))
            result = cursor.fetchone()

            if result:
                user_id = result[0]
                # Use parameterized query to prevent SQL injection
                unsubscribe_query = "UPDATE users SET subscribed = 0 WHERE id = %s"
                cursor.execute(unsubscribe_query, (user_id,))
                db.commit()
                logging.info(f"Unsubscribed email successfully.")
                return "Successfully unsubscribed."
            else:
                logging.info(f"Email not found.")
                return "Email not found in our records.", 404

        except MySQLdb.Error as e:
            db.rollback()
            logging.error(f"Database error: {e}")
            return "An error occurred while unsubscribing. Please try again later.", 500
        finally:
            cursor.close()
            db.close()

    # Generate CSRF token for GET requests
    csrf_token = generate_csrf_token()
    return f"""
        <form method="post">
        <input type="hidden" name="_csrf_token" value="{csrf_token}">
        Email: <input type="email" name="email">
        <button type="submit">Unsubscribe</button>
        </form>
        """

if __name__ == '__main__':
    # Disable debug mode in production
    app.run(debug=False, host='0.0.0.0', port=5000)
