

from flask import Flask, request, redirect, abort
from werkzeug.security import generate_password_hash
import re
import MySQLdb
import os

app = Flask(__name__)

# Ensure required environment variables are present at startup
required_env_vars = ['DB_HOST', 'DB_USER', 'DB_PASSWORD', 'DB_NAME']
for var in required_env_vars:
    if not os.environ.get(var):
        # In production you might log this to a file or monitoring system
        raise RuntimeError(f"Missing required environment variable: {var}")

@app.route('/register', methods=['POST'])
def register():
    # 1. Safely retrieve and normalize inputs
    username = request.form.get('username', '', type=str).strip()
    password = request.form.get('password', '', type=str)

    # 2. Input presence check
    if not username or not password:
        # Generic 400 Bad Request; no user data echoed back
        abort(400)

    # 3. Username validation: allow only 3–30 characters, letters, digits, underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)

    # 4. Password policy enforcement
    #    - Minimum length 8
    #    - At least one uppercase, one lowercase, one digit, one special character
    if (len(password) < 8
        or not re.search(r'[A-Z]', password)
        or not re.search(r'[a-z]', password)
        or not re.search(r'\d', password)
        or not re.search(r'[\W_]', password)):
        abort(400)

    # 5. Securely hash the password using PBKDF2-HMAC-SHA256 with per-user salt
    #    Werkzeug’s generate_password_hash uses a safe default salt_length
    password_hash = generate_password_hash(
        password,
        method='pbkdf2:sha256',
        salt_length=16
    )

    # 6. Insert into database using parameterized queries
    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            use_unicode=True,
            connect_timeout=5
        )
        cursor = db.cursor()

        insert_sql = """
            INSERT INTO users (username, password_hash)
            VALUES (%s, %s)
        """
        cursor.execute(insert_sql, (username, password_hash))
        db.commit()

    except MySQLdb.IntegrityError as e:
        # IntegrityError typically raised on duplicate key (username)
        # Do not reveal which constraint failed. Return HTTP 409 Conflict.
        app.logger.info("Registration attempt with existing username.")
        abort(409)

    except Exception as e:
        # Log full exception details server-side for diagnosis
        app.logger.error("Unexpected error during registration: %s", str(e))
        # Return generic 500 Internal Server Error
        abort(500)

    finally:
        # Clean up DB resources
        try:
            if cursor:
                cursor.close()
            if db:
                db.close()
        except Exception:
            pass  # Suppress any cleanup errors

    # 7. On success, redirect to the login page (no user data included)
    return redirect('/login')
