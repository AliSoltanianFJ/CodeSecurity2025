# ...existing code...
from flask import Flask, request, redirect
import re
import os
import MySQLdb
import bcrypt

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    """
    Register a new user:
    - validate username and password
    - hash password with bcrypt (salted, adaptive)
    - store using a parameterized query
    - use DB credentials from environment variables
    """
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic validation
    if not username or not password:
        return "Username and password required", 400

    # Username: 3-30 chars, letters, digits or underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username format", 400

    # Password policy: minimum length and basic complexity
    if len(password) < 12:
        return "Password must be at least 12 characters", 400
    if not (re.search(r'[A-Z]', password) and re.search(r'[a-z]', password) and re.search(r'\d', password)):
        return "Password must include upper, lower and digit", 400

    # Hash the password with bcrypt (automatically salts)
    try:
        salt = bcrypt.gensalt(rounds=12)
        password_hash = bcrypt.hashpw(password.encode('utf-8'), salt).decode('utf-8')
    except Exception:
        return "Failed to hash password", 500

    # Load DB config from environment (do not hard-code credentials)
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'appuser')
    db_pass = os.environ.get('DB_PASS', 'apppass')
    db_name = os.environ.get('DB_NAME', 'appdb')

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4', use_unicode=True)
        cur = conn.cursor()
        # Use parameterized query to avoid SQL injection
        cur.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        # e.g. duplicate username (assumes unique constraint on username)
        return "Username already exists", 409
    except Exception:
        return "Internal server error", 500
    finally:
        if cur:
            try:
                cur.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    # Registration successful
    return redirect('/login')
# ...existing code...
