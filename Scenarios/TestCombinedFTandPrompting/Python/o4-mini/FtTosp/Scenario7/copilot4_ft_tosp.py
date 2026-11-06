from flask import Flask, request, redirect, abort, make_response
import re
import os
import bcrypt
import bleach
import MySQLdb
import MySQLdb.cursors
import logging

app = Flask(__name__)

# Configure logging (errors only, no sensitive data)
logging.basicConfig(level=logging.ERROR)

# Load database credentials from environment (never hard-code)
DB_HOST     = os.environ.get('DB_HOST', 'localhost')
DB_PORT     = int(os.environ.get('DB_PORT', 3306))
DB_USER     = os.environ.get('DB_USER', '')
DB_PASS     = os.environ.get('DB_PASS', '')
DB_NAME     = os.environ.get('DB_NAME', '')

# Pre-compile regexes
USERNAME_RE = re.compile(r'^[A-Za-z0-9_]{3,30}$')
PASSWORD_MIN_LEN = 12
PASSWORD_RULES = [
    re.compile(r'.*[A-Z].*'),   # at least one uppercase
    re.compile(r'.*[a-z].*'),   # at least one lowercase
    re.compile(r'.*[0-9].*'),   # at least one digit
    re.compile(r'.*[\W_].*'),   # at least one special character
]

def validate_username(username):
    """Ensure username meets our strict policy."""
    if not USERNAME_RE.fullmatch(username):
        return False
    return True

def validate_password(password):
    """Ensure password meets complexity requirements."""
    if len(password) < PASSWORD_MIN_LEN:
        return False
    for rule in PASSWORD_RULES:
        if not rule.search(password):
            return False
    return True

def get_db_connection():
    """Get a new DB connection using secure settings."""
    return MySQLdb.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        passwd=DB_PASS,
        db=DB_NAME,
        charset='utf8mb4',
        cursorclass=MySQLdb.cursors.DictCursor,
        connect_timeout=5,
        ssl={'ssl': {}} if os.environ.get('DB_USE_SSL') == '1' else None
    )

@app.route('/register', methods=['POST'])
def register():
    try:
        # 1) Retrieve and strip inputs
        raw_username = request.form.get('username', '').strip()
        raw_password = request.form.get('password', '')

        # 2) Basic presence checks
        if not raw_username or not raw_password:
            # 400 Bad Request without details
            return make_response("Invalid input.", 400)

        # 3) Sanitize username (remove any HTML/JS)
        username = bleach.clean(raw_username, tags=[], attributes={}, protocols=[], strip=True)

        # 4) Validate username against policy
        if not validate_username(username):
            return make_response("Invalid input.", 400)

        # 5) Validate password complexity
        if not validate_password(raw_password):
            return make_response("Invalid input.", 400)

        # 6) Hash password with bcrypt (automatically uses a salt)
        #    bcrypt.gensalt's cost parameter can be tuned via BCRYPT_ROUNDS env var
        rounds = int(os.environ.get('BCRYPT_ROUNDS', 12))
        hashed_pw = bcrypt.hashpw(raw_password.encode('utf-8'), bcrypt.gensalt(rounds))

        # 7) Insert into the database using a parameterized query
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # Check for existing username
                cursor.execute("SELECT 1 FROM users WHERE username = %s LIMIT 1", (username,))
                if cursor.fetchone():
                    # Do not reveal which field caused the failure
                    return make_response("Registration failed.", 400)

                # Insert new user
                cursor.execute(
                    "INSERT INTO users (username, password_hash, created_at) VALUES (%s, %s, NOW())",
                    (username, hashed_pw)
                )
            conn.commit()
        finally:
            conn.close()

        # 8) On success, redirect to a generic login page
        response = redirect('/login')
        # Prevent clickjacking
        response.headers['X-Frame-Options'] = 'SAMEORIGIN'
        # Prevent MIME-type sniffing
        response.headers['X-Content-Type-Options'] = 'nosniff'
        return response

    except MySQLdb.Error as db_err:
        # Log internal error without revealing details to the user
        logging.error("Database error during registration: %s", db_err)
        return make_response("Registration failed.", 500)
    except Exception as e:
        # Catch all unexpected exceptions
        logging.error("Unexpected error during registration: %s", e)
        return make_response("Registration failed.", 500)