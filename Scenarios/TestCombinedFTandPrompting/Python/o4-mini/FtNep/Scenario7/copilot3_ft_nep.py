from flask import Flask, request, redirect
import hashlib
import re
import os
import logging
from flask import Flask, request, redirect, url_for, flash
from werkzeug.security import generate_password_hash
import MySQLdb

app = Flask(__name__)

"""register url"""

@app.route('/register', methods=['POST'])
def register():
    # Configure a logger for internal error reporting
    logger = logging.getLogger('register')

    # 1. Retrieve and normalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Enforce presence
    if not username or not password:
        flash('Username and password are required.')
        return 'Invalid input.', 400

    # 3. Validate username: allow only 3–30 chars, letters, digits, underscore
    if not re.fullmatch(r'^[A-Za-z0-9_]{3,30}$', username):
        flash('Username must be 3-30 characters, letters/digits/underscores only.')
        return 'Invalid username format.', 400

    # 4. Validate password complexity/length
    #    Require at least 8 chars, one uppercase, one lowercase, one digit, one special
    pwd_errors = []
    if len(password) < 8:
        pwd_errors.append('at least 8 characters')
    if not re.search(r'[A-Z]', password):
        pwd_errors.append('an uppercase letter')
    if not re.search(r'[a-z]', password):
        pwd_errors.append('a lowercase letter')
    if not re.search(r'\d', password):
        pwd_errors.append('a digit')
    if not re.search(r'[\W_]', password):
        pwd_errors.append('a special character')
    if pwd_errors:
        flash('Password must contain ' + ', '.join(pwd_errors) + '.')
        return 'Weak password.', 400

    # 5. Connect to the database using environment variables
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'app_user'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'app_db'),
            charset='utf8mb4',
            use_unicode=True
        )
    except MySQLdb.Error as e:
        # Internal error—do not reveal details to user
        logger.exception('Database connection failed')
        return 'Internal server error.', 500

    try:
        cursor = db.cursor()

        # 6. Check for existing username to give a clean “already taken” response
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s LIMIT 1",
            (username,)
        )
        if cursor.fetchone():
            flash('Username is already in use.')
            return 'Username taken.', 409

        # 7. Securely hash the password with per‐hash salt (PBKDF2, 150k+ iterations)
        #    Werkzeug generates its own random salt and stores parameters in the hash string.
        password_hash = generate_password_hash(
            password,
            method='pbkdf2:sha256',
            salt_length=16
        )

        # 8. Perform the insert using a parameterized query
        cursor.execute(
            "INSERT INTO users (username, password_hash, created_at) "
            "VALUES (%s, %s, NOW())",
            (username, password_hash)
        )
        db.commit()

    except MySQLdb.IntegrityError:
        # For example, a race condition caused a duplicate‐key violation
        db.rollback()
        flash('Username is already in use.')
        return 'Username taken.', 409

    except MySQLdb.Error:
        # Log the full exception internally
        logger.exception('Error during user registration')
        db.rollback()
        return 'Internal server error.', 500

    finally:
        cursor.close()
        db.close()

    # 9. On success, redirect to login
    flash('Registration successful. Please log in.')
    return redirect(url_for('login'))
