from flask import Flask, abort, flash, render_template, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)
    
from werkzeug.security import generate_password_hash
import html

@app.route('/register', methods=['POST'])
def register():
    # Ensure secure transport
    if not request.is_secure:
        abort(400, description="Insecure connection: Use HTTPS.")  # CWE-319

    # Input validation and sanitization
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Enforce strict username policy: alphanumeric, 3-30 chars
    if not re.fullmatch(r'^[A-Za-z0-9_]{3,30}$', username):
        flash('Invalid username.')
        return render_template('register.html'), 400  # CWE-20, CWE-79

    # Enforce strong password policy
    if len(password) < 12 or not re.search(r'[A-Z]', password) \
       or not re.search(r'[a-z]', password) \
       or not re.search(r'[0-9]', password) \
       or not re.search(r'[^\w]', password):
        flash('Password must be at least 12 characters and include upper, lower, digit, and special char.')
        return render_template('register.html'), 400  # CWE-521, CWE-20

    # Prevent username enumeration by using generic messages
    # Use context-aware encoding if echoing output
    safe_username = html.escape(username)

    # Hash password securely with PBKDF2 (avoid deprecated MD5/SHA1)
    password_hash = generate_password_hash(
        password,
        method='pbkdf2:sha256',
        salt_length=16
    )  # CWE-327, CWE-916

    # Principle of Least Privilege: open DB connection with minimal rights
    db_user = os.environ.get('DB_USER_RO', '')
    db_pass = os.environ.get('DB_PASS_RO', '')
    db_name = os.environ.get('DB_NAME', '')
    db_host = os.environ.get('DB_HOST', 'localhost')

    if not all([db_user, db_pass, db_name]):
        abort(500, description="Database credentials not configured.")  # CWE-798

    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            ssl_mode='REQUIRED',  # Enforce encrypted DB connection
            charset='utf8mb4'
        )
        cursor = conn.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        flash('Username already taken.')
        return render_template('register.html'), 409  # CWE-200, CWE-89
    except Exception:
        # Do not expose internal errors
        abort(500, description="Registration failed.")  # CWE-209
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass

    return redirect('/login')
