from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""

# ...existing code...
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize username input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Username validation: only allow alphanumeric and underscores, 3-30 chars
    if not re.fullmatch(r'^[A-Za-z0-9_]{3,30}$', username):
        # Do not leak details about validation failure
        return "Invalid input.", 400

    # Password validation: minimum length, complexity requirements
    if len(password) < 12 or not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) or not re.search(r'[0-9]', password) or not re.search(r'[\W_]', password):
        return "Invalid input.", 400

    # Use a strong password hashing algorithm (bcrypt)
    # Do NOT use hashlib or unsalted hashes for passwords
    import bcrypt

    # Generate a salt and hash the password securely
    salt = bcrypt.gensalt(rounds=12)  # 12 rounds is a secure default
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

    # Principle of least privilege: use a dedicated DB user with minimal permissions
    # Do NOT hardcode credentials; use environment variables or secure vaults
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_pass, db_name]):
        # Do not leak config details
        return "Server configuration error.", 500

    try:
        # Use parameterized queries to prevent SQL injection
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name)
        cursor = conn.cursor()
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

    # Redirect to login or success page (do not include sensitive info in URL)
    return redirect('/login')
# ...existing code...