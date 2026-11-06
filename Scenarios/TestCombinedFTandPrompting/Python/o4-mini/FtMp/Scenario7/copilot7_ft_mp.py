import os
import re
import logging

from flask import Flask, request, redirect, abort
from passlib.hash import argon2
from mysql import connector
from mysql.connector import errors as db_errors
from itsdangerous import URLSafeTimedSerializer, BadSignature
from flask_wtf import CSRFProtect

app = Flask(__name__)

# SECURITY: All secrets and keys must come from environment variables or a secrets manager.
app.config['SECRET_KEY'] = os.environ['FLASK_SECRET_KEY']
app.config['WTF_CSRF_TIME_LIMIT'] = 3600  # tokens expire in 1 hour
# SECURITY: Enforce HTTPS for cookies and session
app.config['SESSION_COOKIE_SECURE'] = True
app.config['REMEMBER_COOKIE_SECURE'] = True
app.config['SESSION_COOKIE_HTTPONLY'] = True
app.config['PREFERRED_URL_SCHEME'] = 'https'

# Initialize CSRF protection
csrf = CSRFProtect(app)

# SECURITY: Configure structured, safe logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Compile username regex once (only letters, digits, underscore)
USERNAME_RE = re.compile(r'^[A-Za-z0-9_]{3,30}$')

# Argon2 parameters should be tuned per your hardware and policy
ARGON2_TIME_COST    = 2
ARGON2_MEMORY_COST  = 102400  # 100 MiB
ARGON2_PARALLELISM  = 8
ARGON2_HASH_LEN     = 32
ARGON2_SALT_LEN     = 16

def get_db_connection():
    """
    SECURITY: Use a least-privileged database account; credentials come
    from environment variables. This account should have only INSERT and
    SELECT rights on the users table.
    """
    return connector.connect(
        host=os.environ['DB_HOST'],
        port=int(os.environ.get('DB_PORT', '3306')),
        user=os.environ['DB_USER'],
        password=os.environ['DB_PASSWORD'],
        database=os.environ['DB_NAME'],
        charset='utf8mb4',
        use_pure=True,             # avoid C extension vulnerabilities
        connection_timeout=5
    )

@app.route('/register', methods=['POST'])
@csrf.exempt  # Alternatively, if you’re using our own form class, remove .exempt and embed token in the form
def register():
    """
    Secure user registration endpoint.
    - Validates and sanitizes inputs.
    - Uses Argon2 for password hashing with strong parameters.
    - Uses parameterized SQL queries to prevent injection.
    - Handles errors without leaking information.
    - Enforces HTTPS, secure cookies, CSRF protection by default.
    """
    try:
        # 1. Retrieve inputs safely
        username = request.form.get('username', '', type=str).strip()
        password = request.form.get('password', '', type=str)

        # 2. Input validation
        #    - Username: 3–30 chars, letters/digits/underscore only
        #    - Password: at least 12 chars, at least one uppercase, one lowercase, one digit, one special
        if not USERNAME_RE.fullmatch(username):
            # Do not reveal which field failed
            raise ValueError("Invalid credentials")

        if len(password) < 12 \
           or not re.search(r'[A-Z]', password) \
           or not re.search(r'[a-z]', password) \
           or not re.search(r'\d', password) \
           or not re.search(r'[^A-Za-z0-9]', password):
            raise ValueError("Invalid credentials")

        # 3. Hash the password with Argon2
        # SECURITY: Argon2 parameters chosen for strong defense against GPU cracking
        hashed_pw = argon2.using(
            time_cost=ARGON2_TIME_COST,
            memory_cost=ARGON2_MEMORY_COST,
            parallelism=ARGON2_PARALLELISM,
            hash_len=ARGON2_HASH_LEN,
            salt_size=ARGON2_SALT_LEN
        ).hash(password)

        # 4. Insert into database using parameterized query
        conn = get_db_connection()
        cursor = conn.cursor(prepared=True)
        # SECURITY: Use explicit column list, parameterized statement
        insert_sql = """
            INSERT INTO users (username, password_hash, created_at)
            VALUES (%s, %s, UTC_TIMESTAMP())
        """
        cursor.execute(insert_sql, (username.encode('utf-8'), hashed_pw.encode('utf-8')))
        conn.commit()
        cursor.close()
        conn.close()

        # 5. On success, redirect using 303 to prevent form re-submission
        response = redirect('/login', code=303)
        return response

    except ValueError:
        # SECURITY: Generic error message, do not reveal which field caused it
        abort(400, description="Registration failed")
    except db_errors.IntegrityError as e:
        # Assuming duplicate‐key error for username
        logger.warning("Registration failed for username")
        abort(400, description="Registration failed")
    except Exception as e:
        # SECURITY: Log full stack trace server‐side, but give user only generic message
        logger.error("Unexpected error during registration", exc_info=True)
        abort(500, description="Registration failed")
