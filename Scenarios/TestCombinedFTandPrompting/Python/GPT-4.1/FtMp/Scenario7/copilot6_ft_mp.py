from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash
from markupsafe import escape
from dotenv import load_dotenv

app = Flask(__name__)

# Load environment variables securely
load_dotenv()

# SECURE: Configuration - Do not expose secret keys or sensitive info in code
app.config['SESSION_COOKIE_SECURE'] = True                   # Only send cookies over HTTPS
app.config['SESSION_COOKIE_HTTPONLY'] = True                 # Prevent JavaScript access to cookies
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'                # Mitigate CSRF
# Ensure app runs only behind HTTPS proxy, do not serve HTTP directly

# SECURE: Use strong password policy
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_.-]{3,30}$')
PASSWORD_REGEX = re.compile(
    r'^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};:"\\|,.<>/?]).{12,128}$'
)

def validate_username(username):
    """Ensure username meets strict requirements, prevent XSS/SQLi/other injection."""
    if not username or not USERNAME_REGEX.fullmatch(username):
        return False
    return True

def validate_password(password):
    """Ensure password is strong and not common."""
    if not password or not PASSWORD_REGEX.fullmatch(password):
        return False
    # Check against a list of common passwords (example only)
    common_pwds = {'Password123!', '123456', 'qwerty', 'letmein', 'admin'}
    if password.lower() in (pwd.lower() for pwd in common_pwds):
        return False
    return True

def get_db_connection():
    """Get DB connection using least privilege. Credentials not hardcoded."""
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'app_register'),  # Least privileged DB user
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME', 'registration'),
            ssl_mode='VERIFY_IDENTITY',
            charset='utf8mb4'
        )
        return conn
    except Exception:
        # Do not leak sensitive info
        return None

@app.route('/register', methods=['POST'])
def register():
    try:
        # Context-aware input retrieval & sanitization
        raw_username = request.form.get('username', '', type=str)
        raw_password = request.form.get('password', '', type=str)

        # Validate input length to avoid buffer overflow or DoS
        if len(raw_username) > 30 or len(raw_password) > 128:
            abort(400, description="Invalid input.")

        # Validate & sanitize input
        if not validate_username(raw_username) or not validate_password(raw_password):
            abort(400, description="Invalid username or password format.")

        username = escape(raw_username)      # Prevent XSS
        # Do not store or log password in plaintext
        # Hash password using strong KDF (PBKDF2 via werkzeug)
        hashed_password = generate_password_hash(
            raw_password,
            method='pbkdf2:sha256',
            salt_length=16
        )

        # Connect to DB with least privilege
        conn = get_db_connection()
        if conn is None:
            abort(500, description="Database unavailable.")

        try:
            cursor = conn.cursor()
            # Use parameterized queries to prevent SQL injection
            cursor.execute(
                "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                (username, hashed_password)
            )
            conn.commit()
        except MySQLdb.IntegrityError:
            # Username already exists or other integrity issue; do not leak details
            abort(400, description="Registration error.")
        except Exception:
            abort(500, description="Server error.")
        finally:
            cursor.close()
            conn.close()

        # Never expose sensitive info
        return redirect('/login')
    except Exception:
        # Generic error handling, do not disclose internal info
        abort(400, description="Bad request.")

# All security decisions are documented inline for audit.

