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
from flask import current_app

app = Flask(__name__)
app.secret_key = secrets.token_hex(32)  # Generate a strong, random secret key

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- Security Headers ---
@app.after_request
def add_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'SAMEORIGIN'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    response.headers['Content-Security-Policy'] = "default-src 'self'"
    response.headers['Referrer-Policy'] = 'strict-origin-when-cross-origin'
    return response

# --- CSRF Protection ---
def generate_csrf_token():
    return secrets.token_hex(16)

app.jinja_env.globals['csrf_token'] = generate_csrf_token

def validate_csrf_token(token):
    return token == session.get('csrf_token')

def csrf_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if request.method == 'POST':
            token = request.form.get('csrf_token')
            if not token or not validate_csrf_token(token):
                logging.warning("CSRF token validation failed.")
                return "CSRF token is missing or invalid.", 403
        return f(*args, **kwargs)
    return decorated_function

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
@csrf_required
def login():
    username = request.form['username']
    password = request.form['password']

    # --- Input Validation ---
    if not username or not password:
        logging.warning("Login attempt with missing username or password.")
        return "Username and password are required.", 400

    if len(username) > 100 or len(password) > 100:
        logging.warning("Login attempt with excessively long username or password.")
        return "Username or password too long.", 400

    # --- Database Connection ---
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # --- Parameterized Query to Prevent SQL Injection ---
        query = "SELECT password_hash, user_id FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if result:
            stored_password_hash, user_id = result
            # --- Password Verification ---
            if check_password_hash(stored_password_hash, password):
                # --- Session Management ---
                session['username'] = username
                session['user_id'] = user_id
                session['csrf_token'] = generate_csrf_token()  # Regenerate CSRF token on login
                logging.info(f"User logged in successfully.")
                return redirect('/home')  # Redirect to home page after successful login
            else:
                logging.warning(f"Failed login attempt - incorrect password.")
                return "Invalid username or password.", 401
        else:
            logging.warning(f"Failed login attempt - user not found.")
            return "Invalid username or password.", 401

    except MySQLdb.Error as e:
        logging.error(f"Database error: {e}")
        return "Internal server error.", 500
    finally:
        if db:
            db.close()

# --- Example Home Route (requires login) ---
@app.route('/home')
def home():
    if 'username' in session:
        return f"Logged in as {session['username']}. <a href='/logout'>Logout</a>"
    else:
        return redirect('/login')

# --- Logout Route ---
@app.route('/logout')
def logout():
    session.pop('username', None)
    session.pop('user_id', None)
    session.pop('csrf_token', None)
    return redirect('/login')

# --- Error Handling ---
@app.errorhandler(404)
def page_not_found(e):
    logging.warning(f"Page not found.")
    return "Page not found", 404

@app.errorhandler(500)
def internal_server_error(e):
    logging.error(f"Internal server error: {e}")
    return "Internal server error", 500

if __name__ == '__main__':
    # --- Development Server Configuration ---
    # Never use 'debug=True' in a production environment!
    app.run(debug=False, host='0.0.0.0', port=5000)
