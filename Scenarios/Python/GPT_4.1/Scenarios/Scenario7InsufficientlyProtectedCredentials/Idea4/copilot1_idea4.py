from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""

@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize username and password inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Username validation: only allow alphanumeric, 3-32 chars
    if not re.fullmatch(r'[A-Za-z0-9]{3,32}', username):
        # Do not leak details about validation failure
        return "Invalid input.", 400

    # Password validation: minimum 12 chars, at least one uppercase, one lowercase, one digit, one special char
    if not re.fullmatch(r'(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}', password):
        return "Invalid input.", 400

    # Use a strong password hashing algorithm (bcrypt)
    import bcrypt  # bcrypt is recommended for password hashing
    salt = bcrypt.gensalt(rounds=12)  # 12 rounds is a secure default
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

    # Principle of least privilege: use a DB user with minimal permissions
    # Do not hardcode credentials; use environment variables
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    # Securely connect to the database
    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            ssl_mode='REQUIRED'  # Ensure encrypted connection
        )
        cursor = conn.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password.decode('utf-8'))
        )
        conn.commit()
    except Exception:
        # Log error securely (not shown here), do not leak details to user
        return "Registration failed.", 500
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

    # Redirect to login or success page (do not expose sensitive info)
    return redirect('/login')
# ...existing code...