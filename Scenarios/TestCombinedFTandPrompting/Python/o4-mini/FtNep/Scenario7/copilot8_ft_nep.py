from flask import Flask, request, redirect, current_app
import re
import os
import MySQLdb
from argon2 import PasswordHasher, exceptions as argon2_exceptions

app = Flask(__name__)

# Configure the Argon2 hasher with reasonable defaults.
# Tweak time_cost / memory_cost / parallelism based on your hardware.
ph = PasswordHasher(
    time_cost=2,         # number of iterations
    memory_cost=102400,  # in kibibytes (100 MiB)
    parallelism=4        # number of parallel threads
)

@app.route('/register', methods=['POST'])
def register():
    # 1) Retrieve and sanitize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2) Validate username: only letters, digits, underscore; 3–30 chars
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username. Only letters, digits and underscore; length 3–30.", 400

    # 3) Validate password length
    if not (12 <= len(password) <= 128):
        return "Password must be between 12 and 128 characters.", 400

    # 4) Validate password complexity: upper, lower, digit, special
    if (not re.search(r'[A-Z]', password) or
        not re.search(r'[a-z]', password) or
        not re.search(r'\d',    password) or
        not re.search(r'\W',    password)):
        return ("Password must include at least one uppercase letter, "
                "one lowercase letter, one digit and one special character."), 400

    # 5) Hash the password with Argon2
    try:
        password_hash = ph.hash(password)
    except argon2_exceptions.HashingError:
        current_app.logger.exception("Argon2 hashing failure")
        return "Internal server error.", 500

    # 6) Connect to MySQL using credentials from the environment
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
    except MySQLdb.MySQLError:
        current_app.logger.exception("Database connection failure")
        return "Internal server error.", 500

    cursor = db.cursor()
    try:
        # 7) Insert user record; assume users.username has a UNIQUE constraint
        cursor.execute(
            "INSERT INTO users (username, password_hash, created_at) "
            "VALUES (%s, %s, NOW())",
            (username, password_hash)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        # e.g. duplicate username
        db.rollback()
        return "Username already taken.", 400
    except MySQLdb.MySQLError:
        db.rollback()
        current_app.logger.exception("Database insertion error")
        return "Internal server error.", 500
    finally:
        cursor.close()
        db.close()

    # 8) On success, redirect to login page
    return redirect('/login')
