from flask import Flask, request, redirect, abort, render_template
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash
from markupsafe import escape

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Enforce HTTPS
    if not request.is_secure:
        abort(403)  # Never process sensitive data over insecure transport

    # Strict input validation and sanitization
    raw_username = request.form.get('username', '')
    raw_password = request.form.get('password', '')

    # Validate username: only allow alphanumeric, length 3-30
    if not re.fullmatch(r'[A-Za-z0-9]{3,30}', raw_username):
        return render_template('error.html', message="Invalid username."), 400

    # Validate password: length and complexity
    if len(raw_password) < 12 or len(raw_password) > 64:
        return render_template('error.html', message="Password must be 12-64 characters."), 400
    if not re.search(r'[A-Z]', raw_password) or not re.search(r'[a-z]', raw_password) \
       or not re.search(r'[0-9]', raw_password) or not re.search(r'[\W_]', raw_password):
        return render_template('error.html', message="Password must include upper, lower, number, symbol."), 400

    # Sanitize username for safe display/storage
    username = escape(raw_username)

    # Never log sensitive information such as passwords

    # Hash password securely using PBKDF2 (via Werkzeug)
    password_hash = generate_password_hash(
        raw_password,
        method='pbkdf2:sha256',
        salt_length=16
    )

    # Principle of least privilege: connect to DB using least privileged user
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', '')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', '')
    if not (db_user and db_pass and db_name):
        abort(500)  # Never expose config details

    try:
        # Do not allow LOCAL INFILE or unsafe options
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            use_unicode=True,
            charset='utf8mb4',
            autocommit=True,
            sql_mode='STRICT_ALL_TABLES'
        )
        cursor = conn.cursor()

        # Use parameterized queries to prevent SQL injection (CWE-89)
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
    except Exception:
        # Generic error message, never leak internal details
        return render_template('error.html', message="Registration failed."), 500
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass  # Suppress secondary errors

    # Do not expose sensitive info in responses
    return redirect('/login')

# Security notes:
# - All inputs are validated/sanitized strictly.
# - Passwords are securely hashed, never stored/transported plaintext.
# - No secrets are hardcoded; configuration via environment variables.
# - SQL injection prevented by parameterized queries.
# - Errors are handled securely, preventing information disclosure.
# - Output is contextually encoded to prevent XSS.
# - User privileges and DB options are minimal and secure.

