import os
import re
import logging
import bcrypt
from flask import Flask, request, redirect, render_template, flash, session
from flask_wtf import CSRFProtect
import MySQLdb
import MySQLdb.cursors

# ========== Application setup and secure defaults ==========

app = Flask(__name__)

# Secret key for session signing – must be set via environment, not in code.
# Ensure this is a long, random value (e.g. 32+ bytes from a CSPRNG).
app.config['SECRET_KEY'] = os.environ.get('FLASK_SECRET_KEY')  
if not app.config['SECRET_KEY']:
    raise RuntimeError("FLASK_SECRET_KEY environment variable is required")

# Secure cookie flags
app.config.update(
    SESSION_COOKIE_SECURE=True,       # send cookies only over HTTPS
    SESSION_COOKIE_HTTPONLY=True,     # disallow JavaScript access to session cookie
    SESSION_COOKIE_SAMESITE='Lax'     # helps mitigate CSRF
)

# Initialize CSRF protection
csrf = CSRFProtect(app)

# Configure application logging (errors only, no sensitive data in messages)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ========== Database connection helper ==========

def get_db_connection():
    """
    Returns a new MySQLdb connection using least-privilege credentials
    specified via environment variables. Client and server must
    communicate over TLS; verify server cert in production.
    """
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASSWORD')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_pass, db_name]):
        raise RuntimeError("Database connection environment variables are not fully set")

    # Connect with SSL (assuming server is configured for TLS)
    conn = MySQLdb.connect(
        host=db_host,
        user=db_user,
        passwd=db_pass,
        db=db_name,
        charset='utf8mb4',
        use_unicode=True,
        ssl={
            'ca': os.environ.get('DB_SSL_CA'),        # path to CA file
            'cert': os.environ.get('DB_SSL_CERT'),    # optional client cert
            'key': os.environ.get('DB_SSL_KEY')       # optional client key
        },
        cursorclass=MySQLdb.cursors.DictCursor,
        autocommit=False       # we will commit explicitly
    )
    return conn

# ========== Input validation patterns and policy ==========

USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,20}$')
PASSWORD_MIN_LENGTH = 12

def is_password_strong(pw: str) -> bool:
    """
    Enforce a strong password policy:
    - At least PASSWORD_MIN_LENGTH characters
    - Contains uppercase, lowercase, digit, and special character
    """
    if len(pw) < PASSWORD_MIN_LENGTH:
        return False
    # Positive lookahead checks
    checks = [
        re.search(r'[A-Z]', pw),
        re.search(r'[a-z]', pw),
        re.search(r'\d', pw),
        re.search(r'[^A-Za-z0-9]', pw)
    ]
    return all(checks)

# ========== Secure register endpoint ==========

@app.route('/register', methods=['GET', 'POST'])
def register():
    """
    Handles user registration securely:
    - Enforces CSRF protection via Flask-WTF
    - Validates and sanitizes all inputs
    - Uses bcrypt with a per-user salt and work factor
    - Stores only the salted hash, never the plaintext password
    - Uses parameterized queries to avoid SQL injection
    - Provides generic error messages to users
    - Logs internal errors without exposing sensitive data
    """
    if request.method == 'GET':
        # Render a template that includes the CSRF token
        return render_template('register.html')

    # POST path: handle form submission
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # ========== Input validation & sanitization ==========

    if not USERNAME_REGEX.fullmatch(username):
        flash("Invalid username. Usernames must be 3–20 characters long and contain only letters, numbers, or underscore.")
        return redirect('/register')

    if not is_password_strong(password):
        flash(f"Password must be at least {PASSWORD_MIN_LENGTH} characters long and include uppercase, lowercase, number, and special character.")
        return redirect('/register')

    # ========== Password hashing ==========

    # Generate a per-user salt with bcrypt's gensalt (cost factor 12).
    # bcrypt automatically handles salting internally.
    try:
        salt = bcrypt.gensalt(rounds=12)
        pw_hash = bcrypt.hashpw(password.encode('utf-8'), salt)
    except Exception as e:
        # Hashing should not fail under normal circumstances
        logger.error("Password hashing failure: %s", e, exc_info=False)
        flash("An internal error occurred. Please try again later.")
        return redirect('/register')

    # ========== Database interaction ==========

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Check for existing username to prevent duplicates
        cursor.execute(
            "SELECT id FROM users WHERE username = %s LIMIT 1",
            (username,)
        )
        if cursor.fetchone():
            flash("Username is already taken.")
            conn.rollback()
            return redirect('/register')

        # Insert new user record
        cursor.execute(
            "INSERT INTO users (username, password_hash, created_at) VALUES (%s, %s, NOW())",
            (username, pw_hash.decode('utf-8'))
        )
        conn.commit()

    except MySQLdb.DatabaseError as db_err:
        # Roll back on any DB error
        try:
            conn.rollback()
        except:
            pass
        # Log internal error without revealing details to user
        logger.error("Database error during registration: %s", db_err, exc_info=False)
        flash("An internal error occurred. Please try again later.")
        return redirect('/register')

    finally:
        try:
            cursor.close()
            conn.close()
        except:
            pass

    # ========== Registration successful ==========

    # Optionally, you might log the event
    logger.info("New user registered successfully")

    flash("Registration successful. You may now log in.")
    return redirect('/login')
