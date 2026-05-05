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

# Securely store the database credentials using environment variables
DB_HOST = os.environ.get("DB_HOST", "localhost")  # Default to localhost if not set
DB_USER = os.environ.get("DB_USER", "root")  # Ensure a strong default user with limited privileges
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")  # Require a strong password
DB_NAME = os.environ.get("DB_NAME", "your_database")  # Use a specific database

# Generate a random secret key for the application
SECRET_KEY = secrets.token_hex(32)
app.config['SECRET_KEY'] = SECRET_KEY

# Configure logging to prevent information leakage
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Strict Transport Security (HSTS) Header
@app.after_request
def add_security_headers(response):
    response.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains; preload'
    response.headers['X-Frame-Options'] = 'SAMEORIGIN'
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    response.headers['Content-Security-Policy'] = "default-src 'self'"
    response.headers['Referrer-Policy'] = 'strict-origin-when-cross-origin'
    return response

# CSRF protection using a double-submit cookie
def generate_csrf_token():
    return secrets.token_hex(16)

def set_csrf_cookie(response):
    csrf_token = generate_csrf_token()
    response.set_cookie('csrf_token', csrf_token, httponly=True, secure=True, samesite='Strict')
    return csrf_token

def validate_csrf_token(request):
    csrf_token_cookie = request.cookies.get('csrf_token')
    csrf_token_form = request.form.get('csrf_token')

    if not csrf_token_cookie or not csrf_token_form or csrf_token_cookie != csrf_token_form:
        return False
    return True

def csrf_protected(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        if request.method == 'POST':
            if not validate_csrf_token(request):
                logging.warning("CSRF validation failed")
                return Response("CSRF validation failed", status=400)
        return fn(*args, **kwargs)
    return wrapper

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET', 'POST'])
@csrf_protected
def unsubscribe():
    if request.method == 'GET':
        # Generate a CSRF token for the form
        csrf_token = generate_csrf_token()
        response = f'''
            <form method="post">
            <input type="hidden" name="csrf_token" value="{csrf_token}">
            Email: <input type="email" name="email"><br>
            <input type="submit" value="Unsubscribe">
            </form>
        '''
        resp = Response(response)
        resp.set_cookie('csrf_token', csrf_token, httponly=True, secure=True, samesite='Strict')
        return resp
    else:
        email = request.form.get('email')

        # Input validation: Check if email is provided and conforms to a basic email format
        if not email:
            logging.warning("Email address not provided.")
            return "Email address is required."

        if '@' not in email or '.' not in email:
            logging.warning(f"Invalid email format.")
            return "Invalid email format."

        try:
            # Establish a secure connection to the database
            db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME, ssl_mode="VERIFY_IDENTITY", ssl={"ca": "/path/to/your/ssl/certificate"})
            cursor = db.cursor()

            # Sanitize the email using a parameterized query to prevent SQL injection
            query = "SELECT * FROM users WHERE email = %s"
            cursor.execute(query, (email,))
            result = cursor.fetchone()

            if result:
                # Unsubscribe the user using a parameterized query
                unsubscribe_query = "DELETE FROM users WHERE email = %s"
                cursor.execute(unsubscribe_query, (email,))
                db.commit()
                logging.info(f"Successfully unsubscribed email: {email}")
                return "Successfully unsubscribed!"
            else:
                logging.info(f"Email not found: {email}")
                return "Email not found."

        except MySQLdb.Error as e:
            db.rollback()
            logging.error(f"Database error: {e}")
            return "An error occurred while unsubscribing."
        except Exception as e:
            logging.error(f"An unexpected error occurred: {e}")
            return "An unexpected error occurred."
        finally:
            if cursor:
                cursor.close()
            if db:
                db.close()

if __name__ == '__main__':
    # Use a production-ready WSGI server like gunicorn or uWSGI in a real deployment
    # Ensure HTTPS is enabled and configured correctly
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 8080)), debug=False)
