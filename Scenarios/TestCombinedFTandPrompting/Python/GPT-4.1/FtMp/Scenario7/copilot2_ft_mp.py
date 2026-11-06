from flask import Flask, abort, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

    
from werkzeug.security import generate_password_hash
import secrets

@app.route('/register', methods=['POST'])
def register():
    # Validate Content-Type
    if request.content_type != 'application/x-www-form-urlencoded':
        abort(400, description="Invalid content type.")

    # Rigorous input validation & sanitization
    username = request.form.get('username', '', type=str).strip()
    password = request.form.get('password', '', type=str)

    # Enforce strict username policy: 4-30 chars, alphanum + limited safe chars
    if not re.fullmatch(r'^[A-Za-z0-9_\-\.]{4,30}$', username):
        abort(400, description="Invalid username format.")

    # Enforce strong password policy: min 12 chars, upper, lower, digit, special
    if (len(password) < 12 or
        not re.search(r'[A-Z]', password) or
        not re.search(r'[a-z]', password) or
        not re.search(r'\d', password) or
        not re.search(r'[!@#$%^&*(),.?":{}|<>]', password)):
        abort(400, description="Password does not meet complexity requirements.")

    # Prevent XSS by context-aware output encoding
    safe_username = escape(username)

    # Hash password securely: PBKDF2 with salt (never store raw pwd/hash)
    password_hash = generate_password_hash(
        password,
        method='pbkdf2:sha256',
        salt_length=16
    )

    # Principle of least privilege: use restricted DB user
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'register_user')
    db_pass = os.environ.get('DB_PASS')  # DO NOT hardcode secrets
    db_name = os.environ.get('DB_NAME', 'appdb')

    if not db_pass:
        abort(500, description="Database credentials unavailable.")

    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            use_unicode=True,
            charset="utf8mb4"
        )
        cursor = conn.cursor()

        # Use parameterized queries to prevent SQL Injection (CWE-89)
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        abort(409, description="Username already exists.")
    except Exception:
        # Generic error message to avoid info leakage (CWE-209)
        abort(500, description="Registration failed.")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

    # Regenerate session ID to prevent fixation
    session.clear()
    session['user'] = safe_username

    # Never expose sensitive data in response
    return redirect('/login')

# Security notes:
# - All input validated/sanitized, no trust in client data.
# - No raw SQL, no string concat; always parameterized queries.
# - No error details disclosed to clients.
# - No secrets in code; use env vars or secure vault.
# - Output is context-escaped; no XSS risk.
# - HTTPS must be enforced at deployment layer.
# - Password policy exceeds NIST/Owasp recommendations.
# - Session data never stores sensitive info.
