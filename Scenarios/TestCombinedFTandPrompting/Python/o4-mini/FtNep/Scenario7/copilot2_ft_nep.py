from flask import Flask, request, redirect, abort
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash

app = Flask(__name__)

# Precompile your username/password policies
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,20}$')
PASSWORD_LENGTH_MIN = 8
PASSWORD_COMPLEXITY = [
    re.compile(r'.*[A-Z].*'),  # uppercase
    re.compile(r'.*[a-z].*'),  # lowercase
    re.compile(r'.*\d.*'),     # digit
    re.compile(r'.*[\W_].*')   # symbol
]

@app.route('/register', methods=['POST'])
def register():
    # 1) Extract and normalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2) Validate username
    if not USERNAME_REGEX.fullmatch(username):
        return "Invalid username. Use 3–20 letters, numbers or underscores only.", 400

    # 3) Validate password length
    if len(password) < PASSWORD_LENGTH_MIN:
        return f"Password must be at least {PASSWORD_LENGTH_MIN} characters long.", 400

    # 4) Validate password complexity
    for regex in PASSWORD_COMPLEXITY:
        if not regex.match(password):
            return ("Password must include uppercase, lowercase, digit and symbol characters.", 400)

    # 5) Hash the password with a per‐user salt and many iterations
    #    PBKDF2 + SHA256, salt_length=16 bytes by default
    try:
        password_hash = generate_password_hash(
            password,
            method='pbkdf2:sha256',
            salt_length=16
        )
    except Exception:
        # Something went wrong during hashing – fail closed
        return "Registration failed.", 500

    # 6) Connect to the database using safe credentials from environment
    db_host = os.getenv('DB_HOST', 'localhost')
    db_user = os.getenv('DB_USER')
    db_pass = os.getenv('DB_PASS')
    db_name = os.getenv('DB_NAME')
    if not all([db_user, db_pass, db_name]):
        # Missing configuration – do not proceed
        return "Service unavailable.", 503

    db = None
    cursor = None
    try:
        db = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            connect_timeout=5
        )
        cursor = db.cursor()

        # 7) Check for existing username
        cursor.execute(
            "SELECT 1 FROM users WHERE username = %s LIMIT 1",
            (username,)
        )
        if cursor.fetchone():
            return "Username already taken.", 409

        # 8) Insert the new user
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()

        # 9) On success, redirect to login
        return redirect('/login', code=303)

    except MySQLdb.OperationalError:
        # DB unavailable or bad credentials
        if db:
            db.rollback()
        return "Service unavailable.", 503

    except MySQLdb.IntegrityError:
        # In case of a race condition inserting the same username
        if db:
            db.rollback()
        return "Username already taken.", 409

    except Exception:
        # Catch all other errors without leaking details
        if db:
            db.rollback()
        return "Registration failed.", 500

    finally:
        # 10) Clean up resources
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if db:
            try:
                db.close()
            except Exception:
                pass
